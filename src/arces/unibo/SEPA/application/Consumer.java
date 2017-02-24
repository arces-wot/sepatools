/* This class abstracts a consumer of the SEPA Application Design Pattern
Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.application;

import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.protocol.SecureEventProtocol.NotificationHandler;


public abstract class Consumer extends Client implements IConsumer,NotificationHandler {
	protected String sparqlSubscribe = null;
	protected String subID ="";
	protected boolean onSubscribe = true;
	protected int DEFAULT_SUBSCRIPTION_TIMEOUT = 3000;
	protected SubcribeConfirmSync subConfirm;
	
	protected String tag = "SEPA CONSUMER";
		
	@Override
	public void semanticEvent(Notification notify) {
		String spuid = notify.getSPUID();
		Integer sequence = notify.getSequence();
		ARBindingsResults results = notify.getARBindingsResults();
				
		BindingsResults added = results.getAddedBindings();
		BindingsResults removed = results.getRemovedBindings();

		//Replace prefixes
		for (Bindings bindings : added.getBindings()) {
			for(String var : bindings.getVariables()) {
				if (bindings.isURI(var)) {
					for(String prefix : URI2PrefixMap.keySet())
						if(bindings.getBindingValue(var).startsWith(prefix)) {
							bindings.addBinding(var, new RDFTermURI(bindings.getBindingValue(var).replace(prefix, URI2PrefixMap.get(prefix)+":")));
							break;
						}
				}
			}
		}
		for (Bindings bindings : removed.getBindings()) {
			for(String var : bindings.getVariables()) {
				if (bindings.isURI(var)) {
					for(String prefix : URI2PrefixMap.keySet())
						if(bindings.getBindingValue(var).startsWith(prefix)) {
							bindings.addBinding(var, new RDFTermURI(bindings.getBindingValue(var).replace(prefix, URI2PrefixMap.get(prefix)+":")));
							break;
						}
				}
			}
		}
		
		if (onSubscribe) {
			onSubscribe = false;
			onSubscribe(added, spuid);
			return;
		}
		
		//Dispatch different notifications based on notify content
		if (!added.isEmpty()) notifyAdded(added,spuid,sequence);
		if (!removed.isEmpty()) notifyRemoved(removed,spuid,sequence);
		notify(results,spuid,sequence);
	}

	@Override
	public void subscribeConfirmed(String spuid) {
		SEPALogger.log(VERBOSITY.DEBUG,tag,"Subscribe confirmed "+spuid);
		subConfirm.notifySubscribeConfirm(spuid);
	}

	@Override
	public void unsubscribeConfirmed(String spuid) {
		SEPALogger.log(VERBOSITY.DEBUG,tag,"Unsubscribe confirmed "+spuid);
	}

	@Override
	public void ping() {
		SEPALogger.log(VERBOSITY.DEBUG,tag,"Ping");
	}
	
	protected class SubcribeConfirmSync {
		private String tag = "SubcribeConfirmSync";
		
		private String subID = "";
		
		public synchronized String waitSubscribeConfirm(int timeout) {
			
			if (!subID.equals("")) return subID;
			
			try {
				SEPALogger.log(VERBOSITY.DEBUG,tag,"Wait for subscribe confirm...");
				wait(timeout);
			} catch (InterruptedException e) {
	
			}
			
			return subID;
		}
		
		public synchronized void notifySubscribeConfirm(String spuid) {
			SEPALogger.log(VERBOSITY.DEBUG,tag,"Notify confirm!");
			
			subID = spuid;
			notifyAll();
		}
	}
	public Consumer(String url,int updatePort,int subscribePort,String path,String SPARQL) {
		super(url,updatePort,subscribePort,path);
		sparqlSubscribe = SPARQL;
	}
	
	public Consumer(ApplicationProfile appProfile,String subscribeID) {
		super(appProfile);
		if (appProfile == null) {
			SEPALogger.log(VERBOSITY.FATAL,tag,"Cannot be initialized with SUBSCRIBE ID: "+subscribeID+ " (application profile is null)");
			return;
		}
		if (appProfile.subscribe(subscribeID) == null) return;
		sparqlSubscribe = appProfile.subscribe(subscribeID);
	}
	
	public String subscribe(Bindings forcedBindings) {
		if (sparqlSubscribe == null) {
			 SEPALogger.log(VERBOSITY.FATAL, tag, "SPARQL SUBSCRIBE not defined");
			 return null;
		 }
		 
		 if (protocolClient == null) {
			 SEPALogger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + replaceBindings(sparqlSubscribe,forcedBindings);
		
		SEPALogger.log(VERBOSITY.DEBUG,tag,"<SUBSCRIBE> ==> "+sparql);
	
		onSubscribe = true;
		
		subConfirm = new SubcribeConfirmSync();
		
		if(!protocolClient.subscribe(sparql, this)) return null;
		
		SEPALogger.log(VERBOSITY.DEBUG,tag,"Wait for subscribe confirm...");
		
		subID = subConfirm.waitSubscribeConfirm(DEFAULT_SUBSCRIPTION_TIMEOUT);
		
		return subID;
		
	}
	 
	public boolean unsubscribe() {
		SEPALogger.log(VERBOSITY.DEBUG,tag,"UNSUBSCRIBE "+subID);
		
		if (protocolClient == null) {
			 SEPALogger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }
		
		return protocolClient.unsubscribe(subID);
	}
}
