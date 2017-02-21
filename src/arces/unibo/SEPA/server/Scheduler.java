/* This class implements the scheduler of the Semantic Event Processing Architecture (SEPA) Engine
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

import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.ErrorResponse;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.QueryRequest;
import arces.unibo.SEPA.commons.QueryResponse;
import arces.unibo.SEPA.commons.Request;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscribeResponse;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeResponse;
import arces.unibo.SEPA.commons.UpdateRequest;
import arces.unibo.SEPA.commons.UpdateResponse;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseListener;

/**
 * This class represents the scheduler of the SUB Engine
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Scheduler extends Thread implements Observer {
	private static String tag = "Scheduler";
	
	private RequestResponseHandler requestHandler;
	private TokenHandler tokenHandler;
	private Processor processor;
	
	private UpdateScheduler updateScheduler = new UpdateScheduler();
	private SubscribeScheduler subscribeScheduler = new SubscribeScheduler();
	private UnsubscribeScheduler unsubscribeScheduler = new UnsubscribeScheduler();
	private QueryScheduler queryScheduler = new QueryScheduler();
		
	private boolean running = true;
	
	public Scheduler(Properties properties,Processor processor) {
		requestHandler = new RequestResponseHandler(properties);
		tokenHandler = new TokenHandler(properties);
		
		if (processor == null) Logger.log(VERBOSITY.ERROR, tag, "Processor is null");
		else {
			this.processor = processor;
			this.processor.addObserver(this);
		}
	}	
	
	private class UpdateScheduler extends Thread {
		@Override
		public void run() {
			while(running){
				Logger.log(VERBOSITY.DEBUG, tag, "Waiting for update requests...");
				UpdateRequest req = requestHandler.waitUpdateRequest();				
				
				//Process UPDATE
				Logger.log(VERBOSITY.DEBUG, tag, ">> UPDATE request #"+req.getToken());
				processor.processUpdate(req);
			}
		}
		
		@Override
		public void start() {
			this.setName("Update Scheduler");
			super.start();
		}
	}
	
	private class QueryScheduler extends Thread {
		@Override
		public void run() {
			while(running){
				Logger.log(VERBOSITY.DEBUG, tag, "Waiting for query requests...");
				QueryRequest req = requestHandler.waitQueryRequest();
				
				//Process QUERY
				Logger.log(VERBOSITY.DEBUG, tag, ">> QUERY request #"+req.getToken());
				processor.processQuery(req);
			}
		}
		
		@Override
		public void start() {
			this.setName("Query Scheduler");
			super.start();
		}
	}

	private class SubscribeScheduler extends Thread {
		@Override
		public void run() {
			while(running){
				Logger.log(VERBOSITY.DEBUG, tag, "Waiting for subscribe requests...");
				SubscribeRequest req = requestHandler.waitSubscribeRequest();
				
				//Process SUBSCRIBE
				Logger.log(VERBOSITY.DEBUG, tag, ">> SUBSCRIBE request #"+req.getToken());
				processor.processSubscribe(req);
			}
		}
		
		@Override
		public void start() {
			this.setName("Subscribe Scheduler");
			super.start();
		}
	}
	
	private class UnsubscribeScheduler extends Thread {
		@Override
		public void run() {			
			while(running){
				Logger.log(VERBOSITY.DEBUG, tag, "Waiting for unsubscribe requests...");
				UnsubscribeRequest req = requestHandler.waitUnsubscribeRequest();				
				
				//Process UNSUBSCRIBE
				Logger.log(VERBOSITY.DEBUG, tag, ">> UNSUBSCRIBE request #"+req.getToken());
				processor.processUnsubscribe(req);
			}
		}
		
		@Override
		public void start() {
			this.setName("Unsubscribe Scheduler");
			super.start();
		}
	}
	
	@Override
	public void start() {
		this.setName("SEPA Scheduler");
		
		updateScheduler.start();
		subscribeScheduler.start();
		unsubscribeScheduler.start();
		queryScheduler.start();
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg.getClass().equals(Notification.class)) {
			Notification notify = (Notification) arg;
			requestHandler.addNotification(notify);
			Logger.log(VERBOSITY.DEBUG, tag, "<< NOTIFICATION ("+notify.getSequence()+") "+notify.getSPUID());
		}
		else if (arg.getClass().equals(QueryResponse.class)) {
			QueryResponse response = (QueryResponse) arg;
			requestHandler.addResponse(response);
			Logger.log(VERBOSITY.DEBUG, tag, "<< QUERY response #"+response.getToken());
		}
		else if (arg.getClass().equals(UpdateResponse.class)) {
			UpdateResponse response = (UpdateResponse) arg;
			requestHandler.addResponse(response);
			Logger.log(VERBOSITY.DEBUG, tag, "<< UPDATE response #"+response.getToken());
		}
		else if (arg.getClass().equals(SubscribeResponse.class)) {
			SubscribeResponse response = (SubscribeResponse) arg;
			requestHandler.addResponse(response);
			Logger.log(VERBOSITY.DEBUG, tag, "<< SUBSCRIBE response #"+response.getToken());
		}
		else if (arg.getClass().equals(UnsubscribeResponse.class)) {
			UnsubscribeResponse response = (UnsubscribeResponse) arg;
			requestHandler.addResponse(response);
			Logger.log(VERBOSITY.DEBUG, tag, "<< UNSUBSCRIBE response #"+response.getToken());
		}
		else if (arg.getClass().equals(ErrorResponse.class)) {
			ErrorResponse response = (ErrorResponse) arg;
			requestHandler.addResponse(response);
			Logger.log(VERBOSITY.WARNING, tag, "<< Error response: #"+response.getToken());
		}
		else {
			Logger.log(VERBOSITY.WARNING, tag, "<< Unsupported response: "+arg.toString());
		}
	}

	public Integer getToken() {
		return tokenHandler.getToken();
	}

	public void addRequest(Request request, ResponseListener listener) {
		requestHandler.addRequest(request, listener);
	}

	public void releaseToken(Integer token) {
		tokenHandler.releaseToken(token);
	}

}
