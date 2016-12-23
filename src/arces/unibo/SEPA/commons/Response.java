package arces.unibo.SEPA.commons;

import com.google.gson.JsonObject;

/**
 * This class represents the response to a generic request.
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class Response {
	protected JsonObject json;
	private Integer token = 0;
	
	public Response(Integer token,JsonObject body) {
		this.token = token;
		json = body;
	}
	
	public String toString() {
		return json.toString();
	}
	
	public Integer getToken() {
		return token;
	}
	
	public JsonObject toJson(){
		return json;
	}
}
