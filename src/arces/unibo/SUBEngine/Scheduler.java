package arces.unibo.SUBEngine;

import java.util.HashMap;
import java.util.Properties;

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;

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
	private SPARQLProtocolClient endpoint;
	private HashMap<String,SPU> spus = new HashMap<String,SPU>();
	
	private UpdateScheduler updateScheduler = new UpdateScheduler();
	private SubscribeScheduler subscribeScheduler = new SubscribeScheduler();
	private UnsubscribeScheduler unsubscribeScheduler = new UnsubscribeScheduler();
	private QueryScheduler queryScheduler = new QueryScheduler();
	
	private SPUScheduler spuScheduler = new SPUScheduler();
	
	private boolean running = true;
	
	public Scheduler(Properties properties,RequestResponseHandler requestHandler,SPARQLProtocolClient endpoint) {
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
				QueryResponse res = endpoint.query(req);
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
				
				synchronized(spus){
					spus.get(req.getSubscribeUUID()).stopRunning();
					spus.remove(req.getSubscribeUUID());
				}
				
				requestHandler.addResponse(new UnsubscribeResponse(req.getToken(),"Unsubscribed"));
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
