package arces.unibo.SEPA.commons;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;

public abstract class RDFTerm {
	protected JsonObject json = null;
	
	public String getValue() {
		return json.get("value").getAsString();
	}
	
	public boolean equals(RDFTerm t) {
		return this.json.equals(t.toJson());
	}

	public JsonObject toJson() {
		return json;
	}
	
	public RDFTerm(String value) {
		json = new JsonObject();
		json.add("value", new JsonPrimitive(value));
	}
	
	public String toString() {
		return json.toString();
	}
}
