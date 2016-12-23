package arces.unibo.SEPA.commons;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This class represents the response of a SPARQL 1.1 Update
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class UpdateResponse extends Response {

	public UpdateResponse(Integer token, String message) {
		super(token,new JsonObject());		
		json.add("message", new JsonPrimitive(message));
	}
}
