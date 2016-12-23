package arces.unibo.SEPA.application;

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.client.SPARQLSEProtocolClient.NotificationHandler;

import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.SPARQLBindingsResults;
import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public abstract class Consumer extends Client implements IConsumer {
	private String SPARQL_SUBSCRIBE = null;
	private String subID ="";

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
					
			SPARQLBindingsResults added = results.getAddedBindings();
			SPARQLBindingsResults removed = results.getRemovedBindings();

			//Dispatch different notifications based on notify content
			if (!added.isEmpty()) consumer.notifyAdded(added,spuid,sequence);
			if (!removed.isEmpty()) consumer.notifyRemoved(removed,spuid,sequence);
			consumer.notify(results,spuid,sequence);
		}
		
	}
	
	public Consumer(SPARQLApplicationProfile appProfile,String subscribeID) {
		super(appProfile);
		if (appProfile == null) {
			Logger.log(VERBOSITY.FATAL,tag,"Cannot be initialized with SUBSCRIBE ID: "+subscribeID+ " (application profile is null)");
			return;
		}
		if (appProfile.subscribe(subscribeID) == null) return;
		SPARQL_SUBSCRIBE = appProfile.subscribe(subscribeID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
		
		handler = new ConsumerHandler(this);
	}
	
	public String subscribe(SPARQLQuerySolution forcedBindings) {
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
