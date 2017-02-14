package arces.unibo.SEPA.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.Bindings;
import arces.unibo.SEPA.commons.BindingsResults;
import arces.unibo.SEPA.commons.ErrorResponse;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.QueryResponse;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscriptionProcessingResult;

public class SPUNaive extends SPU{
	private BindingsResults lastBindings;
	private Integer sequence = 0;
	private String tag ="SPUNaive";
	
	// logging
	Logger logger = LogManager.getRootLogger();
	
	public SPUNaive(SubscribeRequest subscribe, Endpoint endpoint) {
		super(subscribe, endpoint);
	}

	@Override
	public void init() {
		//Get first query results
		Response ret = subscription.queryProcessor.process(subscription.subscribe);
		if (ret.getClass().equals(ErrorResponse.class)) return;		
		QueryResponse queryResults = (QueryResponse) ret;
		
		//Notify bindings
		lastBindings = queryResults.getBindingsResults();
		if (!lastBindings.isEmpty()) {
			ARBindingsResults bindings =  new ARBindingsResults(lastBindings,null);
			Notification notification = new Notification(getUUID(),bindings,sequence++);
			setChanged();
			notifyObservers(notification);	
		}	
	}

	@Override
	public SubscriptionProcessingResult process(SubscriptionProcessingInputData update) {
		logger.debug(getUUID() + " Start processing");
		Response ret = subscription.queryProcessor.process(subscription.subscribe);
		
		Notification notification = null;
		
		if (ret.getClass().equals(ErrorResponse.class)) {				
			return new SubscriptionProcessingResult(getUUID(),notification);	
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
			notification = new Notification(getUUID(),bindings,sequence++);
		}
		
		lastBindings = new BindingsResults(newBindings);
		
		SubscriptionProcessingResult res = new SubscriptionProcessingResult(getUUID(),notification);
		
		logger.debug(getUUID() + " End processing");
		
		return res;	
	}

}
