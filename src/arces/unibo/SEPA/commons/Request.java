package arces.unibo.SEPA.commons;

/**
 * This class represents a generic request (i.e., QUERY, UPDATE, SUBSCRIBE, UNSUBSCRIBE)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class Request {
	private Integer token;
	private String sparql;
	
	public Request(Integer token,String sparql) {
		this.token = token;
		this.sparql = sparql;
	}
	
	public Integer getToken() {
		return token;
	}
	
	public String getSPARQL() {
		return sparql;
	}
}
