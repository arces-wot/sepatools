/* This class implements the interface between the input gates (e.g. HTTP, WS) and the scheduler/SPUs
    Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.server;

import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.QueryRequest;
import arces.unibo.SEPA.commons.Request;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscribeResponse;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UpdateRequest;
import arces.unibo.SEPA.commons.UpdateResponse;

/**
 * This class implements the handler of the different requests: QUERY, UPDATE, SUBSCRIBE, UNSUBSCRIBE
 * 
 * It also used to notify interested listeners of new responses
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class RequestResponseHandler {
	private String tag = "RequestResponseHandler";
	
	public interface ResponseListener {
		public void notifyResponse(Response response);
	}
	
	//Request queues
	private ConcurrentLinkedQueue<UpdateRequest> updateRequestQueue = new ConcurrentLinkedQueue<UpdateRequest>();
	private ConcurrentLinkedQueue<SubscribeRequest> subscribeRequestQueue = new ConcurrentLinkedQueue<SubscribeRequest>();
	private ConcurrentLinkedQueue<QueryRequest> queryRequestQueue = new ConcurrentLinkedQueue<QueryRequest>();
	private ConcurrentLinkedQueue<UnsubscribeRequest> unsubscribeRequestQueue = new ConcurrentLinkedQueue<UnsubscribeRequest>();
	
	//Update response queue
	private ConcurrentLinkedQueue<UpdateResponse> updateResponseQueue = new ConcurrentLinkedQueue<UpdateResponse>();
	
	//Response listeners
	private HashMap<Integer,ResponseListener> listeners = new HashMap<Integer,ResponseListener>();
	private HashMap<String,ResponseListener> subscribers = new HashMap<String,ResponseListener>();
	
	//Single update processing (sequential subscriptions processing)
	private static int subscriptionsChecked = 0;
	
	public RequestResponseHandler(Properties properties){
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
	}
	
	public synchronized void addResponse(Response response) {
		Integer token = response.getToken();
		Logger.log(VERBOSITY.DEBUG, tag, "Add response #"+token);
		
		ResponseListener listener = listeners.get(token);
		if (listener == null) Logger.log(VERBOSITY.WARNING, tag, "Listener is null"); 
		else {
			listener.notifyResponse(response);
		}
		listeners.remove(token);
		
		if (response.getClass().equals(SubscribeResponse.class)) {
			subscribers.put(((SubscribeResponse) response).getSPUID(),listener);
		}
		else if (response.getClass().equals(UpdateResponse.class)) {	
			updateResponseQueue.offer((UpdateResponse)response);
			notifyAll();
		}
	}
	
	public void addNotification(Notification notification) {
		subscribers.get(notification.getSPUID()).notifyResponse(notification);
	}
	
	public synchronized void addRequest(Request req,ResponseListener listener) {
		//Register response listener
		listeners.put(req.getToken(), listener);
		
		//Add request to the request queue
		if (req.getClass().equals(QueryRequest.class)) {
			Logger.log(VERBOSITY.INFO, tag, "Add QUERY request #"+req.getToken());
			queryRequestQueue.offer((QueryRequest)req);
		}
		else if (req.getClass().equals(UpdateRequest.class)) {
			Logger.log(VERBOSITY.INFO, tag, "Add UPDATE request #"+req.getToken());
			updateRequestQueue.offer((UpdateRequest)req);
		}
		else if (req.getClass().equals(SubscribeRequest.class)) {
			Logger.log(VERBOSITY.INFO, tag, "Add SUBSCRIBE request #"+req.getToken());
			subscribeRequestQueue.offer((SubscribeRequest)req);
		}
		else {
			Logger.log(VERBOSITY.INFO, tag, "Add UNSUBSCRIBE request #"+req.getToken());
			unsubscribeRequestQueue.offer((UnsubscribeRequest)req);
		}
		
		notifyAll();
	}
	
	public synchronized UpdateResponse waitUpdateResponse() {
		UpdateResponse res;
		
		while((res = updateResponseQueue.poll())==null) 
			try {
				wait();
			}
			catch (InterruptedException e) {}
		
		return res;
	}
	
	public synchronized QueryRequest waitQueryRequest() {
		QueryRequest req;
		while ((req = queryRequestQueue.poll()) == null) {
			try {
				wait();
			}
			catch (InterruptedException e) {}
		}
		return req;
	}
	
	public synchronized UpdateRequest waitUpdateRequest() {
		UpdateRequest req;
		while ((req = updateRequestQueue.poll()) == null) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		return req;
	}
		
	public synchronized SubscribeRequest waitSubscribeRequest() {
		SubscribeRequest req;
		while ((req = subscribeRequestQueue.poll()) == null) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		return req;
	}
	
	public synchronized UnsubscribeRequest waitUnsubscribeRequest() {
		UnsubscribeRequest req;
		while ((req = unsubscribeRequestQueue.poll()) == null) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		return req;
	}
	
	public synchronized void waitAllSubscriptionChecks(int n) {
		subscriptionsChecked = 0;
		while (subscriptionsChecked != n) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
	}
	
	public synchronized void subscriptionCheckEnded() {
		subscriptionsChecked++;
		notifyAll();
	}
}
