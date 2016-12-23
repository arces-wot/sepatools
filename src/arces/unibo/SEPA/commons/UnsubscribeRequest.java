package arces.unibo.SEPA.commons;

public class UnsubscribeRequest extends Request {
	
	public UnsubscribeRequest(Integer token, String subId) {
		super(token, subId);
	}
	
	public String getSubscribeUUID(){
		return super.getSPARQL();
	}

}
