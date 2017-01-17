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

import java.util.HashMap;
import java.util.Properties;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.QueryRequest;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeResponse;
import arces.unibo.SEPA.commons.UpdateRequest;
import arces.unibo.SEPA.commons.UpdateResponse;

/**
 * This class represents the scheduler of the SUB Engine
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Scheduler extends Thread {
	private static String tag = "Scheduler";
	
	private RequestResponseHandler requestHandler;
	private Endpoint endpoint;
	private HashMap<String,SPU> spus = new HashMap<String,SPU>();
	
	private UpdateScheduler updateScheduler = new UpdateScheduler();
	private SubscribeScheduler subscribeScheduler = new SubscribeScheduler();
	private UnsubscribeScheduler unsubscribeScheduler = new UnsubscribeScheduler();
	private QueryScheduler queryScheduler = new QueryScheduler();
	
	private SPUScheduler spuScheduler = new SPUScheduler();
	
	private boolean running = true;
	
	public Scheduler(Properties properties,RequestResponseHandler requestHandler,Endpoint endpoint) {
		this.endpoint = endpoint;
		if (endpoint == null) Logger.log(VERBOSITY.ERROR, tag, "Endpoint is null");
		
		this.requestHandler = requestHandler;
		if (requestHandler == null) Logger.log(VERBOSITY.ERROR, tag, "Request handler is null");
		
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
	}	
	
	private class SPUScheduler extends Thread {
		@Override
		public void run() {
			while(running){
				Logger.log(VERBOSITY.DEBUG, tag, "Waiting for update response...");
				UpdateResponse res = requestHandler.waitUpdateResponse();
				
				Logger.log(VERBOSITY.DEBUG, tag, "Check for notifications");
				
				synchronized(spus) {
					for (SPU spu : spus.values()) spu.check4Notification(res);
				}
			}
		}
		
		@Override
		public void start() {
			this.setName("SPU Scheduler");
			super.start();
		}	
	}
	
	private class UpdateScheduler extends Thread {
		@Override
		public void run() {
			while(running){
				Logger.log(VERBOSITY.DEBUG, tag, "Waiting for update requests...");
				UpdateRequest req = requestHandler.waitUpdateRequest();
				
				Logger.log(VERBOSITY.DEBUG, tag, "New update request: "+req.getSPARQL());
				UpdateResponse res = endpoint.update(req);
				requestHandler.addResponse(res);
				
				//Single UPDATE processing
				requestHandler.waitAllSubscriptionChecks(spus.entrySet().size());
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
				
				Logger.log(VERBOSITY.DEBUG, tag, "New query request: "+req.getSPARQL());
				Response res = endpoint.query(req);
				requestHandler.addResponse(res);
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
				
				Logger.log(VERBOSITY.DEBUG, tag, "New subscribe request: "+req.getSPARQL());
				
				SPU spu = new SPU(req,endpoint,requestHandler);
				
				synchronized(spus) {
					spus.put(spu.getUUID(),spu);
				}
				spu.setName("SPU_"+spu.getUUID());
				spu.start();
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
				
				Logger.log(VERBOSITY.DEBUG, tag, "New unsubscribe request: "+req.getSPARQL());
				
				String spuid = req.getSubscribeUUID();
				
				synchronized(spus){
					if (spus.containsKey(spuid)){
						spus.get(spuid).stopRunning();
						spus.remove(spuid);
					}
				}
				
				requestHandler.addResponse(new UnsubscribeResponse(req.getToken(),spuid));
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
		spuScheduler.start();
	}
}
