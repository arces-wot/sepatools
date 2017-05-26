/* This class abstracts a consumer of the SEPA Application Design Pattern
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

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

package arces.unibo.SEPA.client.pattern;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.NotificationHandler;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;

public abstract class Consumer extends Client implements IConsumer,NotificationHandler {
	protected String sparqlSubscribe = null;
	protected String subID ="";
	protected boolean onSubscribe = true;
	protected int DEFAULT_SUBSCRIPTION_TIMEOUT = 3000;
	protected SubcribeConfirmSync subConfirm;
	
	private static final Logger logger = LogManager.getLogger("Consumer");
		
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
	public void subscribeConfirmed(SubscribeResponse response) {
		logger.debug("Subscribe confirmed "+response.getSpuid()+ " alias: "+response.getAlias());
		subConfirm.notifySubscribeConfirm(response.getSpuid());
	}

	@Override
	public void unsubscribeConfirmed(UnsubscribeResponse response) {
		logger.debug("Unsubscribe confirmed "+response.getSpuid());
	}

	@Override
	public void ping() {
		logger.debug("Ping");
	}
	
	protected class SubcribeConfirmSync {		
		private String subID = "";
		
		public synchronized String waitSubscribeConfirm(int timeout) {
			
			if (!subID.equals("")) return subID;
			
			try {
				logger.debug("Wait for subscribe confirm...");
				wait(timeout);
			} catch (InterruptedException e) {
	
			}
			
			return subID;
		}
		
		public synchronized void notifySubscribeConfirm(String spuid) {
			logger.debug("Notify confirm!");
			
			subID = spuid;
			notifyAll();
		}
	}
	
	public Consumer(ApplicationProfile appProfile,String subscribeID) throws IllegalArgumentException {
		super(appProfile);

		if (appProfile == null || subscribeID == null) {
			logger.fatal("One or more arguments are null");
			throw new IllegalArgumentException("One or more arguments are null");
		}

		if (appProfile.subscribe(subscribeID) == null) {
			logger.fatal("SUBSCRIBE ID " + subscribeID + " not found in " + appProfile.getFileName());
			throw new IllegalArgumentException("SUBSCRIBE ID " + subscribeID + " not found in " + appProfile.getFileName());
		}
		
		sparqlSubscribe = appProfile.subscribe(subscribeID);			
	}
	
	public Consumer(String jparFile) throws IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException {
		super(jparFile);
	}

	public String subscribe(Bindings forcedBindings) throws IOException, URISyntaxException {
		if (sparqlSubscribe == null) {
			logger.fatal( "SPARQL SUBSCRIBE not defined");
			 return null;
		 }
		 
		 if (protocolClient == null) {
			 logger.fatal("Client not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + replaceBindings(sparqlSubscribe,forcedBindings);
		
		logger.debug("<SUBSCRIBE> ==> "+sparql);
	
		onSubscribe = true;
		
		subConfirm = new SubcribeConfirmSync();
		
		Response response = protocolClient.subscribe(new SubscribeRequest(sparql), this);

		logger.debug(response.toString());
		
		if(response.getClass().equals(ErrorResponse.class)) return null;
		
		logger.debug("Wait for subscribe confirm...");
		
		subID = subConfirm.waitSubscribeConfirm(DEFAULT_SUBSCRIPTION_TIMEOUT);
		
		return subID;
		
	}
	 
	public boolean unsubscribe() throws IOException, URISyntaxException {
		logger.debug("UNSUBSCRIBE "+subID);
		
		if (protocolClient == null) {
			logger.fatal("Client not initialized");
			 return false;
		 }
		
		Response response;

		response = protocolClient.unsubscribe(new UnsubscribeRequest(subID));

		logger.debug(response.toString());
		
		return !(response.getClass().equals(ErrorResponse.class));
	}
}
