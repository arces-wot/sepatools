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

package arces.unibo.SEPA.server;

import java.util.UUID;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.ErrorResponse;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.QueryResponse;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.BindingsResults;
import arces.unibo.SEPA.commons.Bindings;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscribeResponse;
import arces.unibo.SEPA.commons.UpdateResponse;

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
	private Endpoint endpoint = null;
	private RequestResponseHandler handler = null;
	
	private boolean running = true;
	
	private String uuid = null;
	private Integer sequence = 0;
	
	private boolean newUpdate = false;
	
	public SPU(SubscribeRequest subscribe,Endpoint endpoint,RequestResponseHandler handler) {
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
		//Send response
		Response ret = endpoint.query(subscribe);
		if (ret.getClass().equals(ErrorResponse.class)) return;		
		QueryResponse queryResults = (QueryResponse) ret;
		
		SubscribeResponse response = new SubscribeResponse(queryResults.getToken(),getUUID());
		handler.addResponse(response);
		
		BindingsResults lastBindings = queryResults.getBindingsResults();
		if (!lastBindings.isEmpty()) {
			ARBindingsResults bindings =  new ARBindingsResults(lastBindings,null);
			Notification notification = new Notification(getUUID(),bindings,sequence++);
			handler.addNotification(notification);	
		}
		
		//Main loop
		Logger.log(VERBOSITY.INFO, tag, getName()+" Entering main loop...");
		while(running){			
			//Wait for a new update
			if(!wait4Update()) return;
			
			ret = endpoint.query(subscribe);
			if (ret.getClass().equals(ErrorResponse.class)) {
				handler.subscriptionCheckEnded();
				continue;		
			}
			QueryResponse currentResults = (QueryResponse) ret;
			
			BindingsResults currentBindings = currentResults.getBindingsResults();
			BindingsResults newBindings = new BindingsResults(currentBindings);
			
			BindingsResults added = new BindingsResults(currentBindings.getVariables(),null);
			BindingsResults removed = new BindingsResults(currentBindings.getVariables(),null);
			
			for(Bindings solution : lastBindings.getBindings()) {
				if(!currentBindings.contains(solution)) removed.add(solution);
				else currentBindings.remove(solution);	
			}
			
			for(Bindings solution : currentBindings.getBindings()) {
				if(!lastBindings.contains(solution)) added.add(solution);	
			}
				
			//Send notification
			if (!added.isEmpty() || !removed.isEmpty()){
				ARBindingsResults bindings =  new ARBindingsResults(added,removed);
				Notification notification = new Notification(getUUID(),bindings,sequence++);
				handler.addNotification(notification);
			}
			
			lastBindings = new BindingsResults(newBindings);
			
			handler.subscriptionCheckEnded();
		}	
	}
	
	public synchronized boolean wait4Update() {
		newUpdate = false;
		while(!newUpdate){
			try {
				Logger.log(VERBOSITY.INFO, tag, getName()+" Waiting new update response...");
				wait();
			} catch (InterruptedException e) {
				if (!newUpdate) return false;
			}
		}
		newUpdate = false;
		return true;
	}
	
	public synchronized void check4Notification(UpdateResponse res) {
		newUpdate = true;
		notifyAll();
	}

}
