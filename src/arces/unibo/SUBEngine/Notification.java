package arces.unibo.SUBEngine;

/**
 * This class represents a SPARQL Notification (see SPARQL 1.1 Notification Language)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Notification extends Response {
	
	private String uuid = null;
	private ARBindingsResults results = null;
	
	public Notification(String uuid,ARBindingsResults results,Integer sequence) {
		super(sequence,results.toString());
		this.uuid = uuid;
		this.results = results;
	}
	
	public String getSPUID() {
		return uuid;
	}
	
	public String getString() {
		return results.toString();
	}

}
