package arces.unibo.SEPA;

import java.util.Vector;

import arces.unibo.KPI.SIBResponse;
import arces.unibo.KPI.SSAP_sparql_response;
import arces.unibo.KPI.iKPIC_subscribeHandler2;
import arces.unibo.SEPA.Logger.VERBOSITY;

public class GenericClient extends Client {
	private String subID ="";
	private iKPIC_subscribeHandler2 mHandler = null;
	private Notification listener = null;
	
	public interface Notification {
		public void notify(BindingsResults notify);
		public void notifyFirst(BindingsResults notify);
	}
	
	public GenericClient(String SIB_IP,int SIB_PORT,String SIB_NAME, Notification listener){
		super(SIB_IP,SIB_PORT,SIB_NAME);
		this.listener = listener; 
	}
	
	public boolean update(String SPARQL_UPDATE,Bindings forced) {
		
		if (!isJoined()) return false;
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_UPDATE,forced).replace("\n", "").replace("\r", "");
		
		ret = kp.update_sparql(sparql);
		
		if (ret == null) return false;
		 
		 return ret.isConfirmed();
	 }
	
	public BindingsResults query(String SPARQL_QUERY,Bindings forced) {
		if (!isJoined()) return null;
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_QUERY,forced).replace("\n", "").replace("\r", "");
		
		SIBResponse ret;

		ret = kp.querySPARQL(sparql);
		
		if (!ret.isConfirmed()) {
			Logger.log(VERBOSITY.FATAL,"SEPA","Query FAILED "+sparql);
			return null;
		}
		
		BindingsResults queryResults = new BindingsResults(ret.sparqlquery_results,null,URI2PrefixMap);
		
		return queryResults;
	}
	
	public String subscribe(String SPARQL_SUBSCRIBE,Bindings forced) {
		if (mHandler != null) return subID;
		
		mHandler = new NotificationHandler();
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_SUBSCRIBE,forced).replace("\n", "").replace("\r", "");
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Subscribe "+sparql);
		

		ret = kp.subscribeSPARQL(sparql, mHandler);

		
		if (ret == null) return null;
		if (!ret.isConfirmed()) return null;
		
		subID = ret.subscription_id;
		
		if(!ret.isConfirmed()) return null;
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Subscribe "+subID);
		
		BindingsResults queryResults = new BindingsResults(ret.sparqlquery_results,null,URI2PrefixMap);
		if (queryResults != null) {
			if (queryResults.getAddedBindings().isEmpty()) return subID;
			
			listener.notifyFirst(queryResults);
		}
		
		return subID;
	}
	 
	public final boolean unsubscribe() {
		Logger.log(VERBOSITY.DEBUG,"SEPA","Unsubscribe "+subID);
		
		ret = kp.unsubscribe(subID);
		
		mHandler = null;
		subID = "";
		
		if (ret == null) return false;		
		
		return ret.isConfirmed();
	}
	
	public class NotificationHandler implements iKPIC_subscribeHandler2{
		public void kpic_RDFEventHandler(Vector<Vector<String>> newTriples, Vector<Vector<String>> oldTriples, String indSequence, String subID ){}
		
		public void kpic_SPARQLEventHandler(SSAP_sparql_response newResults, SSAP_sparql_response oldResults, String indSequence, String subID ){	
			BindingsResults results = new BindingsResults(newResults,oldResults,URI2PrefixMap);
			listener.notify(results);
		}
		
		public void kpic_UnsubscribeEventHandler(String sub_ID ){}
		
		public void kpic_ExceptionEventHandler(Throwable SocketException ){}
	}
}
