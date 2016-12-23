package arces.unibo.SEPA.application;

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public abstract class Aggregator extends Consumer implements IAggregator {
	private String sparqlUpdate = "INSERT { ?subject ?predicate ?object }";
	private String updateID = "";
	private String tag = "SEPA AGGREGATOR";
	
	public Aggregator(SPARQLApplicationProfile appProfile,String subscribeID,String updateID){
		super(appProfile,subscribeID);
		if (appProfile == null){
			Logger.log(VERBOSITY.FATAL,tag,"Cannot be initialized with UPDATE ID " +updateID+" (application profile is null)");
			return;	
		}
		if (appProfile.update(updateID) == null) {
			Logger.log(VERBOSITY.FATAL,tag,"UPDATE ID " +updateID+" not found");
			return;
		}
		
		sparqlUpdate = appProfile.update(updateID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
		this.updateID = updateID;
	} 
		
	public boolean update(SPARQLQuerySolution forcedBindings){
		 
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.ERROR,tag,"UPDATE " +updateID+" FAILED because client has not been inizialized");
			 return false;
		 }
		 
		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);
		 
		 Logger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+updateID+" ==> "+sparql);
		 
		 return protocolClient.update(sparql);
	 }
}
