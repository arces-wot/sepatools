package arces.unibo.SUBEngine;

public class UnsubscribeRequest extends Request {

	public UnsubscribeRequest(Integer token, String sparql) {
		super(token, sparql);
	}
	
	public String getSubscribeUUID(){
		return "";
	}

}
