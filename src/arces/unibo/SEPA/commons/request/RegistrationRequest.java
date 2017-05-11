package arces.unibo.SEPA.commons.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RegistrationRequest {
	JsonObject json = new JsonObject();
	
	public RegistrationRequest(String id) {
		json.add("client_identity", new JsonPrimitive(id));
		JsonArray grants = new JsonArray();
		grants.add("client_credentials");
		json.add("grant_types", grants);
	}
	
	public String toString() {
		return json.toString();
	}

}
