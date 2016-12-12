package arces.unibo.SUBEngine;

import java.util.UUID;

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class SPU extends Thread {
	private static String tag ="SPU";
	
	private SubscribeRequest subscribe = null;
	private boolean running = true;
	private ARTriples matchingTriples = null;
	
	private LUTT lutt = null;
	private CTS cts = null;
	private SPARQLProtocolClient endpoint = null;
	private Booster booster = null;
	private RequestResponseHandler handler = null;
	private String uuid = null;
	private Integer sequence = 0;
	
	public SPU(SubscribeRequest subscribe,SPARQLProtocolClient endpoint,RequestResponseHandler handler) {
		this.subscribe = subscribe;
		this.endpoint = endpoint;
		this.handler = handler;
		uuid = UUID.randomUUID().toString();
	}
	
	public void stopRunning() {
		running = false;
		interrupt();
	}
	
	public String getUUID() {
		return uuid;
	}
	
	@Override
	public void run() {
		//Create LUTT
		Logger.log(VERBOSITY.INFO, tag, getName()+" Create LUTT");
		lutt = new LUTT(subscribe);
		
		//Create CTS
		Logger.log(VERBOSITY.INFO, tag, getName()+" Create CTS");
		cts = new CTS(subscribe,endpoint);
		
		//Create BOOSTER
		Logger.log(VERBOSITY.INFO, tag, getName()+" Create BOOSTER");
		booster = new Booster(cts,subscribe);
		
		//Send response
		handler.addResponse(cts.getQueryResults());
		
		//Main loop
		Logger.log(VERBOSITY.INFO, tag, getName()+" Entering main loop...");
		while(running){			
			//Wait for a new update
			matchingTriples = new ARTriples(null,null);
			synchronized(matchingTriples){
				while(matchingTriples.isEmpty()){
					try {
						Logger.log(VERBOSITY.INFO, tag, getName()+" Waiting new update response...");
						matchingTriples.wait();
					} catch (InterruptedException e) {}
				}
			}
			if (!running) return;	
			
			//Booster
			Logger.log(VERBOSITY.INFO, tag, getName()+" BOOSTER");
			ARBindingsResults bindings = booster.run(matchingTriples);
			
			//Send notification
			if (bindings != null){
				Notification notification = new Notification(getUUID(),bindings,sequence++);
				handler.addNotification(notification);
			}
		}	
	}
	
	public synchronized void check4Notification(UpdateResponse res) {
		//LUTT filtering
		Logger.log(VERBOSITY.INFO, tag, getName()+" LUTT filtering");
		matchingTriples = lutt.matching(res.getARTriples());	
		if (matchingTriples.isEmpty()) return;
		matchingTriples.notify();
	}

}
