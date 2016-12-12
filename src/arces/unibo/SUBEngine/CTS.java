package arces.unibo.SUBEngine;

/**
 * This class represents the Context Triple Store (CTS) used by SPUs (Semantic Processing Unit)
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class CTS {
	QueryResponse response;
	
	public CTS(SubscribeRequest subscribe,SPARQLProtocolClient endpoint) {
		//TODO to be implemented
		response = endpoint.query(new QueryRequest(subscribe.getToken(),subscribe.getSPARQL()));
	}
	
	public SPARQLBindingsResults query(SubscribeRequest query) {
		//TODO to be implemented
		return null;
	}

	public void update(ARTriples triples) {
		// TODO to be implemented
		
	}
	
	public QueryResponse getQueryResults(){
		return response;
	}
}
