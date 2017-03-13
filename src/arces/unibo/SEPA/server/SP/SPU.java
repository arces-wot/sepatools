/* This class implements a Semantic Processing Unit (SPU) of the Semantic Event Processing Architecture (SEPA) Engine
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

package arces.unibo.SEPA.server.SP;

import java.util.Observable;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.SubscriptionProcessingResult;
import arces.unibo.SEPA.commons.response.UpdateResponse;
import arces.unibo.SEPA.server.Endpoint;
import arces.unibo.SEPA.server.QueryProcessor;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class SPU extends Observable implements Runnable {
	private static final Logger logger = LogManager.getLogger("SPU");	
	private String uuid = null;
	private ConcurrentLinkedQueue<SubscriptionProcessingInputData> spuData = new ConcurrentLinkedQueue<SubscriptionProcessingInputData>();
	private boolean running = true;
	protected SubscriptionProcessingInputData subscription = new SubscriptionProcessingInputData();
	
	class SubscriptionProcessingInputData {
		public UpdateResponse update = null;
		public QueryProcessor queryProcessor = null;
		public SubscribeRequest subscribe = null;	
	}
	
	public SPU(SubscribeRequest subscribe,Endpoint endpoint) {
		uuid = UUID.randomUUID().toString();
		subscription.subscribe = subscribe;
		subscription.queryProcessor = new QueryProcessor(endpoint);
		spuData.offer(subscription);
	}
	
	public synchronized void stopRunning() {
		running = false;
		notifyAll();
	}
	
	public String getUUID() {
		return uuid;
	}
	
	//To be implemented by specific implementations
	public abstract void init();
	public abstract SubscriptionProcessingResult process(SubscriptionProcessingInputData update);
	
	public synchronized void subscriptionCheck(UpdateResponse res) {
		subscription.update = res;
		spuData.offer(subscription);
		notifyAll();
	}
	
	private synchronized SubscriptionProcessingInputData waitUpdate() {
		while(spuData.isEmpty()){
			try {
				logger.debug(getUUID() + " Waiting new update response...");
				wait();
			} catch (InterruptedException e) {}
			
			if (!running) return null;
		}
		
		return spuData.poll();	
	}
	@Override
	public void run() {
		//Notify subscription ID (SPU ID)
		SubscriptionProcessingInputData request = spuData.poll();
		SubscribeResponse response = new SubscribeResponse(request.subscribe.getToken(),getUUID());
		setChanged();
		notifyObservers(response);
			
		init();
		
		//Main loop
		logger.debug(getUUID()+" Entering main loop...");
		while(running){			
			//Wait new update
			SubscriptionProcessingInputData update = waitUpdate();
			
			if (update == null && !running) return;
			
			//Processing
			SubscriptionProcessingResult result = process(update);
			
			//Results notification
			setChanged();
			notifyObservers(result);
		}	
	}
}
