package arces.unibo.SEPA.commons;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class UnsubscribeResponse extends Response {
	
	public UnsubscribeResponse(Integer token, String SPUID) {
		super(token, new JsonObject());
		
		JsonPrimitive jsonSpuid = new JsonPrimitive(SPUID);
		json.add("spuid", jsonSpuid);
	}
	
	public String getSPUID() {
		return super.toJson().get("spuid").getAsString();
	}

}
