package arces.unibo.SEPA.commons;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This class represents the response to a SPARQL 1.1 Subscribe
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class SubscribeResponse extends Response {
	
	public SubscribeResponse(Integer token,String spuid) {
		super(token,new JsonObject());
		json.add("spuid", new JsonPrimitive(spuid));
	}

	public String getSPUID() {
		return json.get("spuid").getAsString();
	}
}
