package arces.unibo.SUBEngine;

/**
 * This class represents a request to perform a SPARQL 1.1 Query
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class QueryRequest extends Request {

	public QueryRequest(Integer token, String sparql) {
		super(token, sparql);
	}

}
