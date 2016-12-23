package arces.unibo.SEPA.application;

import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQLBindingsResults;
import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public interface IConsumer extends IClient{	
	public String subscribe(SPARQLQuerySolution forcedBindings);
	public boolean unsubscribe();
	
	public void notify(ARBindingsResults notify,String spuid,Integer sequence);
	public void notifyAdded(SPARQLBindingsResults bindingsResults,String spuid,Integer sequence);
	public void notifyRemoved(SPARQLBindingsResults bindingsResults,String spuid,Integer sequence);
	public void onSubscribe(SPARQLBindingsResults bindingsResults,String spuid);
}
