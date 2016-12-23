package arces.unibo.SEPA.application;

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.client.SPARQLSEProtocolClient.NotificationHandler;
import arces.unibo.SEPA.commons.SPARQLBindingsResults;
import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public class GenericClient extends Client {
	private String subID = null;
	private NotificationHandler handler;
	private String tag = "Generic client";
	
	public GenericClient(String url,int updatePort,int subscribePort,String path,NotificationHandler handler){
		super(url,updatePort,subscribePort,path);
		this.handler = handler;
	}
	
	public boolean update(String SPARQL_UPDATE,SPARQLQuerySolution forced) {
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }
		 
		String sparql = prefixes() + super.replaceBindings(SPARQL_UPDATE,forced).replace("\n", "").replace("\r", "");
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Update "+sparql);
		
		return protocolClient.update(sparql);
	 }
	
	public SPARQLBindingsResults query(String SPARQL_QUERY,SPARQLQuerySolution forced) {
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_QUERY,forced).replace("\n", "").replace("\r", "");

		Logger.log(VERBOSITY.DEBUG,"SEPA","QUERY "+sparql);
		
		return protocolClient.query(sparql);
	}
	
	public String subscribe(String SPARQL_SUBSCRIBE,SPARQLQuerySolution forced) {	
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		if (subID != null) {
			 Logger.log(VERBOSITY.ERROR, tag, "Client is subscribed. First unsubscribe "+subID);
			 return null;
		 }
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_SUBSCRIBE,forced).replace("\n", "").replace("\r", "");
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Subscribe "+sparql);
		
		subID = protocolClient.subscribe(sparql, handler);
		
		return subID;
	}
	 
	public boolean unsubscribe() {
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		}
		
		if (subID == null) {
			 Logger.log(VERBOSITY.ERROR, tag, "Client is not subscribed");
			 return false;
		}
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Unsubscribe "+subID);
		
		boolean ret = protocolClient.unsubscribe(subID);
		
		if (ret) subID = null;
		
		return ret;
	}
}
