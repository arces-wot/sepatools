package arces.unibo.SEPA;

import arces.unibo.SEPA.Logger.VERBOSITY;

public abstract class Aggregator extends Consumer implements IAggregator {
	private String SPARQL_UPDATE = "INSERT { ?subject ?predicate ?object }";
	private String UPDDATE_ID = "";
	private String tag = "SEPA AGGREGATOR";
	
	public Aggregator(String subscribeQuery,String updateQuery,String SIB_IP,int SIB_PORT,String SIB_NAME){
		super(subscribeQuery,SIB_IP,SIB_PORT,SIB_NAME);
		SPARQL_UPDATE = updateQuery.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
	}
	
	public Aggregator(String subscribeID,String updateID){
		super(subscribeID);
		SPARQL_UPDATE = SPARQLApplicationProfile.update(updateID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
		UPDDATE_ID = updateID;
	} 
		
	public boolean update(Bindings forcedBindings){
		 if (!isJoined()) {
			 Logger.log(VERBOSITY.ERROR,tag,"UPDATE " +UPDDATE_ID+" FAILED because client is not joined");
			 return false;
		 }
		 
		 String sparql = prefixes() + replaceBindings(SPARQL_UPDATE,forcedBindings);
		 
		 Logger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+UPDDATE_ID+" ==> "+sparql);
		 
		 ret = kp.update_sparql(sparql);
		 
		 if (ret == null) {
			 Logger.log(VERBOSITY.ERROR,tag,"Update return value is NULL");
			 return false;
		 }
		 
		if(!ret.isConfirmed()) Logger.log(VERBOSITY.ERROR,tag,"UPDATE "+UPDDATE_ID+" FAILED "+sparql);

		 return ret.isConfirmed();
	 }
}
