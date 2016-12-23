package arces.unibo.SEPA.commons;

import com.google.gson.JsonObject;

/**
 * This class represents the results of a SPARQL 1.1 Query
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class QueryResponse extends Response {
	
	public QueryResponse(Integer token, JsonObject body) {
		super(token, body);
	}
	
	public SPARQLBindingsResults getBindingsResults() {
		return new SPARQLBindingsResults(super.toJson());
	}
	
}
