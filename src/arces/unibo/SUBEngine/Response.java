package arces.unibo.SUBEngine;

/**
 * This class represents the response to a generic request
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class Response {
	private Integer token;
	private String response;
	
	public Response(Integer token,String response) {
		this.token = token;
		this.response = response;
	}
	
	public String getString() {
		return response;
	}
	
	public Integer getToken() {
		return token;
	}
}
