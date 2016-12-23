package arces.unibo.SEPA.commons;

import com.google.gson.JsonPrimitive;

/**
 * This class represents a SPARQL Notification (see SPARQL 1.1 Notification Language)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Notification extends Response {
	private String spuid;
	private ARBindingsResults results;
	private Integer sequence;
	
	public Notification(String spuid,ARBindingsResults results,Integer sequence) {
		super(0,results.toJson());
		
		JsonPrimitive jsonSpuid = new JsonPrimitive(spuid);
		JsonPrimitive jsonSequence = new JsonPrimitive(sequence);
		super.toJson().add("spuid", jsonSpuid);
		super.toJson().add("sequence", jsonSequence);
		
		this.spuid = spuid;
		this.results = results;
		this.sequence = sequence;
	}
	
	public String getSPUID() {
		return spuid;
	}
	
	public String toString() {
		return super.toString();
	}

	public ARBindingsResults getARBindingsResults() {
		return results;
	}

	public Integer getSequence() {
		return sequence;
	}
}
