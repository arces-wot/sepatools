package arces.unibo.SEPA.commons;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ErrorResponse extends Response {

	public ErrorResponse(Integer token,String message) {
		super(token,new JsonObject());		
		json.add("message", new JsonPrimitive(message));
	}

}
