package arces.unibo.SUBEngine;

/**
 * This class represents the request of performing a SPARQL 1.1 Subscribe
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class SubscribeRequest extends QueryRequest {

	public SubscribeRequest(Integer token, String sparql) {
		super(token, sparql);
	}

}
