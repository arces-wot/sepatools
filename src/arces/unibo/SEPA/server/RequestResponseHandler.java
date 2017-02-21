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
import arces.unibo.SEPA.commons.QueryResponse;
import arces.unibo.SEPA.commons.Request;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscribeResponse;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeResponse;
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
	private HashMap<Integer,ResponseListener> responseListeners = new HashMap<Integer,ResponseListener>();
	private HashMap<String,ResponseListener> subscribers = new HashMap<String,ResponseListener>();
	
	public RequestResponseHandler(Properties properties){
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
	}
	
	/**
	 * This method add the response (e.g, UPDATE, QUERY, SUBSCRIBE, UNSUBSCRIBE)
	 * 
	 * @see Response
	* */
	public void addResponse(Response response) {
		Integer token = response.getToken();
		
		//Notify listener
		ResponseListener listener = responseListeners.get(token);
		if (listener != null) listener.notifyResponse(response);
		responseListeners.remove(token);
		
		if (response.getClass().equals(SubscribeResponse.class)) {
			Logger.log(VERBOSITY.DEBUG, tag, "SUBSCRIBE response #"+token);
			subscribers.put(((SubscribeResponse) response).getSPUID(),listener);
		}
		else if (response.getClass().equals(UpdateResponse.class)) {	
			Logger.log(VERBOSITY.DEBUG, tag, "UPDATE response #"+token);
			synchronized(updateResponseQueue) {
				updateResponseQueue.offer((UpdateResponse)response);
				updateResponseQueue.notifyAll();
			}
		}
		else if (response.getClass().equals(UnsubscribeResponse.class)) {	
			Logger.log(VERBOSITY.DEBUG, tag, "UNSUBSCRIBE response #"+token);
		}
		else if (response.getClass().equals(QueryResponse.class)) {	
			Logger.log(VERBOSITY.DEBUG, tag, "QUERY response #"+token);
		}
	}
	
	/**
	 * This method add a notification sent by a SPU
	 * 
	 * @see Notification
	* */
	public void addNotification(Notification notification) {
		Logger.log(VERBOSITY.DEBUG, tag, "NOTIFICATION ("+notification.getSequence()+") "+notification.getSPUID());
		subscribers.get(notification.getSPUID()).notifyResponse(notification);
	}
	
	/**
	 * This method is used by producers (e.g. HTTP Gate) to add a request (e.g, UPDATE, QUERY, SUBSCRIBE, UNSUBSCRIBE). The registered listener will receive a notification when the request will be completed
	 * 
	 * @see Request, ResponseListener
	* */
	public void addRequest(Request req,ResponseListener listener) {
		//Register response listener
		responseListeners.put(req.getToken(), listener);
		
		//Add request to the right queue
		if (req.getClass().equals(QueryRequest.class)) {
			
			synchronized(queryRequestQueue) {
				Logger.log(VERBOSITY.DEBUG, tag, "QUERY request #"+req.getToken());
				queryRequestQueue.offer((QueryRequest)req);
				queryRequestQueue.notifyAll();
			}
		}
		else if (req.getClass().equals(UpdateRequest.class)) {
			
			synchronized(updateRequestQueue) {
				Logger.log(VERBOSITY.DEBUG, tag, "UPDATE request #"+req.getToken());
				updateRequestQueue.offer((UpdateRequest)req);
				updateRequestQueue.notifyAll();
			}
		}
		else if (req.getClass().equals(SubscribeRequest.class)) {
			
			synchronized(subscribeRequestQueue) {
				Logger.log(VERBOSITY.DEBUG, tag, "SUBSCRIBE request #"+req.getToken());
				subscribeRequestQueue.offer((SubscribeRequest)req);
				subscribeRequestQueue.notifyAll();
			}
		}
		else {
			
			synchronized(unsubscribeRequestQueue) {
				Logger.log(VERBOSITY.DEBUG, tag, "UNSUBSCRIBE request #"+req.getToken());
				unsubscribeRequestQueue.offer((UnsubscribeRequest)req);
				unsubscribeRequestQueue.notifyAll();
			}
		}
	}
	
	/**
	 * This method blocks until a new UPDATE response has been added
	 * 
	 * @see UpdateResponse
	* */
	public UpdateResponse waitUpdateResponse() {
		UpdateResponse res;
			
		synchronized(updateResponseQueue) {
			while((res = updateResponseQueue.poll()) == null)
				try {
					Logger.log(VERBOSITY.DEBUG, tag, "Waiting for UPDATE responses...");
					updateResponseQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return res;
	}
	
	/**
	 * This method blocks until a new QUERY request has been added
	 * 
	 * @see QueryRequest
	* */
	public QueryRequest waitQueryRequest() {
		QueryRequest req;
		
		synchronized(queryRequestQueue) {
			while((req = queryRequestQueue.poll()) == null)
				try {
					Logger.log(VERBOSITY.DEBUG, tag, "Waiting for QUERY requests...");
					queryRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
	
	/**
	 * This method blocks until a new UPDATE request has been added
	 * 
	 * @see UpdateRequest
	* */
	public UpdateRequest waitUpdateRequest() {
		UpdateRequest req;
		
		synchronized(updateRequestQueue) {
			while((req = updateRequestQueue.poll()) == null)
				try {
					Logger.log(VERBOSITY.DEBUG, tag, "Waiting for UPDATE requests...");
					updateRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
	
	/**
	 * This method blocks until a new SUBSCCRIBE request has been added
	 * 
	 * @see SubscribeRequest
	* */
	public SubscribeRequest waitSubscribeRequest() {
		SubscribeRequest req;
		
		synchronized(subscribeRequestQueue) {
			while((req = subscribeRequestQueue.poll()) == null)
				try {
					Logger.log(VERBOSITY.DEBUG, tag, "Waiting for SUBSCRIBE requests...");
					subscribeRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
	
	/**
	 * This method blocks until a new UNSUBSCCRIBE request has been added
	 * 
	 * @see UnsubscribeRequest
	* */

	public UnsubscribeRequest waitUnsubscribeRequest() {
		UnsubscribeRequest req;
		
		synchronized(unsubscribeRequestQueue) {
			while((req = unsubscribeRequestQueue.poll()) == null)
				try {
					Logger.log(VERBOSITY.DEBUG, tag, "Waiting for UNSUBSCRIBE requests...");
					unsubscribeRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
}
