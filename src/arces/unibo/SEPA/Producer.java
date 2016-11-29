package arces.unibo.SEPA;

import arces.unibo.SEPA.Logger.VERBOSITY;

public class Producer extends Client implements IProducer {
	private String SPARQL_UPDATE = null;
	private String SPARQL_ID = "";
	private String tag = "SEPA PRODUCER";
	
	public Producer(String updateQuery,String SIB_IP,int SIB_PORT,String SIB_NAME){
		super(SIB_IP,SIB_PORT,SIB_NAME);
		SPARQL_UPDATE = updateQuery.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();;
	}
	
	public Producer(SPARQLApplicationProfile appProfile,String updateID){
		super(appProfile);
		SPARQL_ID = updateID;
		if (appProfile.update(updateID) == null) {
			Logger.log(VERBOSITY.FATAL, tag, "Cannot find UPDATE ID: "+updateID);
			return;
		}
		SPARQL_UPDATE = appProfile.update(updateID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();;
	}
	
	public boolean update(Bindings forcedBindings){
		 if (!isJoined()) {
			 Logger.log(VERBOSITY.ERROR,tag,"UPDATE "+SPARQL_ID+" FAILED because client is not joined");
			 return false;
		 }
		 
		 if (SPARQL_UPDATE == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "SPARQL UPDATE not defined");
			 return false;
		 }
		 String sparql = prefixes() + replaceBindings(SPARQL_UPDATE,forcedBindings);
		 
		 Logger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+ SPARQL_ID+" ==> "+sparql);
		 
		 ret = kp.update_sparql(sparql);
		 
		 if (ret == null) {
			 Logger.log(VERBOSITY.ERROR,tag,"Update return value is NULL");
			 return false;
		 }
		 
		if(!ret.isConfirmed()) Logger.log(VERBOSITY.ERROR,tag,"UPDATE "+ SPARQL_ID+" FAILED "+sparql);

		 return ret.isConfirmed();
	 }
}
