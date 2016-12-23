package arces.unibo.SEPA.application;

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public class Producer extends Client implements IProducer {
	private String SPARQL_UPDATE = null;
	private String SPARQL_ID = "";
	private String tag = "SEPA PRODUCER";
	
	public Producer(String updateQuery,String url,int updatePort,int subscribePort,String path){
		super(url,updatePort,subscribePort,path);
		SPARQL_UPDATE = updateQuery.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
	}
	
	public Producer(SPARQLApplicationProfile appProfile,String updateID){
		super(appProfile);
		if (appProfile == null) {
			Logger.log(VERBOSITY.FATAL, tag, "Cannot be initialized with UPDATE ID: "+updateID+" (application profile is null)");
			return;
		}
		if (appProfile.update(updateID) == null) {
			Logger.log(VERBOSITY.FATAL, tag, "Cannot find UPDATE ID: "+updateID);
			return;
		}
		
		SPARQL_ID = updateID;
		
		SPARQL_UPDATE = appProfile.update(updateID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
	}
	
	public boolean update(SPARQLQuerySolution forcedBindings){	 
		 if (SPARQL_UPDATE == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "SPARQL UPDATE not defined");
			 return false;
		 }
		 
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }

		 String sparql = prefixes() + replaceBindings(SPARQL_UPDATE,forcedBindings);
		 
		 Logger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+ SPARQL_ID+" ==> "+sparql);
		 
		 return protocolClient.update(sparql);
	 }
}
