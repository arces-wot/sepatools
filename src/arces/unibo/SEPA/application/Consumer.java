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

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.client.SecureEventProtocol.NotificationHandler;

import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.RDFTermURI;
import arces.unibo.SEPA.commons.BindingsResults;
import arces.unibo.SEPA.commons.Bindings;

public abstract class Consumer extends Client implements IConsumer {
	private String SPARQL_SUBSCRIBE = null;
	private String subID ="";
	private boolean onSubscribe = true;
	
	private String tag = "SEPA CONSUMER";
	private ConsumerHandler handler;
	
	class ConsumerHandler implements NotificationHandler {
		private Consumer consumer;
		
		public ConsumerHandler(Consumer consumer) {
			this.consumer = consumer;
		}
		
		@Override
		public void notify(Notification notify) {
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
				consumer.onSubscribe(added, spuid);
				return;
			}
			
			//Dispatch different notifications based on notify content
			if (!added.isEmpty()) consumer.notifyAdded(added,spuid,sequence);
			if (!removed.isEmpty()) consumer.notifyRemoved(removed,spuid,sequence);
			consumer.notify(results,spuid,sequence);
		}
		
	}
	
	public Consumer(ApplicationProfile appProfile,String subscribeID) {
		super(appProfile);
		if (appProfile == null) {
			Logger.log(VERBOSITY.FATAL,tag,"Cannot be initialized with SUBSCRIBE ID: "+subscribeID+ " (application profile is null)");
			return;
		}
		if (appProfile.subscribe(subscribeID) == null) return;
		SPARQL_SUBSCRIBE = appProfile.subscribe(subscribeID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
		
		handler = new ConsumerHandler(this);
	}
	
	public String subscribe(Bindings forcedBindings) {
		if (SPARQL_SUBSCRIBE == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "SPARQL SUBSCRIBE not defined");
			 return null;
		 }
		 
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + replaceBindings(SPARQL_SUBSCRIBE,forcedBindings);
		
		Logger.log(VERBOSITY.DEBUG,tag,"<SUBSCRIBE> ==> "+sparql);
	
		 onSubscribe = true;
		 
		return protocolClient.subscribe(sparql, handler);
	}
	 
	public boolean unsubscribe() {
		Logger.log(VERBOSITY.DEBUG,tag,"UNSUBSCRIBE "+subID);
		
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }
		
		return protocolClient.unsubscribe(subID);
	}
}
