package arces.unibo.SUBEngine;

/**
 * This class represents the response to a SPARQL 1.1 Subscribe
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class SubscribeResponse extends Response {
	private String SPUID = null;
	
	public SubscribeResponse(Integer token, String response,String SPUID) {
		super(token, response);
		this.SPUID = SPUID;
	}
	
	public String getSPUID() {
		return SPUID;
	}

}
