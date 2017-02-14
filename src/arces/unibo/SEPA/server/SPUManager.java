/* This class implements the manager of the Semantic Processing Units (SPUs) of the Semantic Event Processing Architecture (SEPA) Engine
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
import java.util.Observable;
import java.util.Observer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscriptionProcessingResult;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UpdateResponse;

public class SPUManager extends Observable implements Observer{
	private String tag ="SPU Manager";
	
	// logging
	Logger logger = LogManager.getRootLogger();
	
	private Endpoint endpoint;
	private HashMap<String,SPU> spus = new HashMap<String,SPU>();

	//Sequential update processing
	private static int subscriptionsChecked = 0;
	
	public SPUManager(Endpoint endpoint) {
		this.endpoint = endpoint;
	}
	
	public void processSubscribe(SubscribeRequest req) {
		logger.debug("Process SUBSCRIBE #"+req.getToken());
		
		//TODO: choose different kind of SPU based on subscription request
		SPU spu = new SPUNaive(req,endpoint);
		spu.addObserver(this);
		
		synchronized(spus) {
			spus.put(spu.getUUID(),spu);
		}
		
		Thread th = new Thread(spu);
		th.setName("SPU_"+spu.getUUID());
		th.start();	
	}
	
	public String processUnsubscribe(UnsubscribeRequest req) {
		logger.debug("Process UNSUBSCRIBE #"+req.getToken());
		String spuid = req.getSubscribeUUID();
		
		synchronized(spus){
			if (spus.containsKey(spuid)){
				spus.get(spuid).stopRunning();
				spus.remove(spuid);
			}
		}
		
		return spuid;
	}
	
	public void processUpdate(UpdateResponse res) {
		logger.debug("*** PROCESSING UPDATE STARTED ***");
		
		//Sequential update processing
		waitAllSubscriptionChecks(res);
		
		logger.debug("*** PROCESSING UPDATE FINISHED ***");
	}

	private synchronized void waitAllSubscriptionChecks(UpdateResponse res) {			
		subscriptionsChecked = 0;
		
		synchronized(spus) {
			//Wake-up all SPUs
			logger.debug("Activate SPUs (Total: "+spus.size()+")");
			for (SPU spu: spus.values()) spu.subscriptionCheck(res);
			
			logger.debug( "Waiting all SPUs to complete processing...");		
			while (subscriptionsChecked != spus.size()) {
				try {
					wait();
				} catch (InterruptedException e) {
					logger.debug( "SPUs processing ended "+subscriptionsChecked+"/"+spus.size());
				}
			}
		}
	}
	
	private synchronized void subscriptionProcessingEnded(){
		subscriptionsChecked++;
		notifyAll();
		logger.debug( "SPU processing ended #"+subscriptionsChecked);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg.getClass().equals(SubscriptionProcessingResult.class)){
			SubscriptionProcessingResult ret = (SubscriptionProcessingResult) arg;
			
			//SPU processing ended
			logger.debug("SPU "+ret.getSPUID()+" proccesing ended");
			subscriptionProcessingEnded();
						
			//Send notification if required
			if (!ret.toBeNotified()) return;
			else {
				logger.debug("Notify observers");
				setChanged();
				notifyObservers(ret.getNotification());
			}
		}
		else {
			logger.debug("Notify observers");
			setChanged();
			notifyObservers(arg);
		}
	}
}
