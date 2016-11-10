package arces.unibo.SEPA;

import arces.unibo.SEPA.Logger.VERBOSITY;

public class Producer extends Client implements IProducer {
	private String SPARQL_UPDATE = "";
	private String SPARQL_ID = "";
	private String tag = "SEPA PRODUCER";
	
	public Producer(String updateQuery,String SIB_IP,int SIB_PORT,String SIB_NAME){
		super(SIB_IP,SIB_PORT,SIB_NAME);
		SPARQL_UPDATE = updateQuery.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();;
	}
	
	public Producer(String updateID){
		super();
		SPARQL_ID = updateID;
		SPARQL_UPDATE = SPARQLApplicationProfile.update(updateID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();;
	}
	
	public boolean update(Bindings forcedBindings){
		 if (!isJoined()) {
			 Logger.log(VERBOSITY.ERROR,tag,"UPDATE "+SPARQL_ID+" FAILED because client is not joined");
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
