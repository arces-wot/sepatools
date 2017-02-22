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

import arces.unibo.SEPA.application.SEPALogger;
import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.QueryResponse;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;
import arces.unibo.SEPA.commons.response.UpdateResponse;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseAndNotificationListener;

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
		
		if (processor == null) SEPALogger.log(VERBOSITY.ERROR, tag, "Processor is null");
		else {
			this.processor = processor;
			this.processor.addObserver(this);
		}
	}	
	
	private class UpdateScheduler extends Thread {
		@Override
		public void run() {
			while(running){
				UpdateRequest req = requestHandler.waitUpdateRequest();				
				
				//Process UPDATE
				SEPALogger.log(VERBOSITY.DEBUG, tag, ">> "+req.toString());
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
				QueryRequest req = requestHandler.waitQueryRequest();
				
				//Process QUERY
				SEPALogger.log(VERBOSITY.DEBUG, tag, ">> "+req.toString());
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
				SubscribeRequest req = requestHandler.waitSubscribeRequest();
				
				//Process SUBSCRIBE
				SEPALogger.log(VERBOSITY.DEBUG, tag, ">> "+req.toString());
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
				UnsubscribeRequest req = requestHandler.waitUnsubscribeRequest();				
				
				//Process UNSUBSCRIBE
				SEPALogger.log(VERBOSITY.DEBUG, tag, ">> "+req.toString());
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
			SEPALogger.log(VERBOSITY.DEBUG, tag, "<< "+notify.toString());
		}
		else if (arg.getClass().equals(QueryResponse.class)) {
			QueryResponse response = (QueryResponse) arg;
			requestHandler.addResponse(response);
			SEPALogger.log(VERBOSITY.DEBUG, tag, "<< "+response.toString());
		}
		else if (arg.getClass().equals(UpdateResponse.class)) {
			UpdateResponse response = (UpdateResponse) arg;
			requestHandler.addResponse(response);
			SEPALogger.log(VERBOSITY.DEBUG, tag, "<< "+response.toString());
		}
		else if (arg.getClass().equals(SubscribeResponse.class)) {
			SubscribeResponse response = (SubscribeResponse) arg;
			requestHandler.addResponse(response);
			SEPALogger.log(VERBOSITY.DEBUG, tag, "<< "+response.toString());
		}
		else if (arg.getClass().equals(UnsubscribeResponse.class)) {
			UnsubscribeResponse response = (UnsubscribeResponse) arg;
			requestHandler.addResponse(response);
			SEPALogger.log(VERBOSITY.DEBUG, tag, "<< "+response.toString());
		}
		else if (arg.getClass().equals(ErrorResponse.class)) {
			ErrorResponse response = (ErrorResponse) arg;
			requestHandler.addResponse(response);
			SEPALogger.log(VERBOSITY.WARNING, tag, "<< "+response.toString());
		}
		else {
			SEPALogger.log(VERBOSITY.WARNING, tag, "<< Unsupported response: "+arg.toString());
		}
	}

	public Integer getToken() {
		return tokenHandler.getToken();
	}

	public void addRequest(Request request, ResponseAndNotificationListener listener) {
		requestHandler.addRequest(request, listener);
	}

	public void releaseToken(Integer token) {
		tokenHandler.releaseToken(token);
	}

}
