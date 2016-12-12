package arces.unibo.SUBEngine;

/**
 * This class represents the response of a SPARQL 1.1 Update
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class UpdateResponse extends Response {

	public UpdateResponse(Integer token, String response) {
		super(token, response);
	}
	
	public ARTriples getARTriples() {
		//TODO: to be implemented
		return null;
	}

}
