package arces.unibo.SUBEngine;

/**
 * This class represents the request to perform a SPARQL 1.1 Update
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class UpdateRequest extends Request {

	public UpdateRequest(Integer token, String sparql) {
		super(token, sparql);
	}

}
