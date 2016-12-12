package arces.unibo.SUBEngine;

public class UnsubscribeResponse extends Response {
	
	private String SPUID = null;
	
	public UnsubscribeResponse(Integer token, String SPUID) {
		super(token, "");
		this.SPUID = SPUID;
	}
	
	public String getSPUID() {
		return SPUID;
	}

}
