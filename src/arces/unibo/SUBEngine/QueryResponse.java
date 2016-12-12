package arces.unibo.SUBEngine;

/**
 * This class represents the results of a SPARQL 1.1 Query
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class QueryResponse extends Response {
	
	private SPARQLBindingsResults results;
	
	public QueryResponse(Integer token, String response) {
		super(token, response);
	}

	public QueryResponse(Integer token, SPARQLBindingsResults results) {
		super(token, results.toString());
		this.results = results;
	}
	
	public SPARQLBindingsResults getBindingsResults() {
		return results;
	}
	
}
