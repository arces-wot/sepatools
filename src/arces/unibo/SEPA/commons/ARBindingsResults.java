package arces.unibo.SEPA.commons;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class represents the content of a SEPA notification
 * 
 * It includes the added and removed bindings since the previous notification
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class ARBindingsResults {
	JsonObject results = new JsonObject();
	
	public ARBindingsResults(JsonObject results) {
		this.results = results;
	} 
	
	public ARBindingsResults(SPARQLBindingsResults added,SPARQLBindingsResults removed) {
		JsonObject nullResults = new JsonObject();
		JsonArray arr = new JsonArray();
		nullResults.add("bindings", arr);
		
		JsonObject nullHead = new JsonObject();
		nullHead.add("vars", new JsonArray());
		
		JsonObject addedResults = nullResults;
		JsonObject removedResults = nullResults;
		JsonObject head = nullHead;
		
		if (added != null) {
			head = added.toJson().get("head").getAsJsonObject();
			addedResults = added.toJson().get("results").getAsJsonObject();
		}
		
		if (removed != null) {
			head = removed.toJson().get("head").getAsJsonObject();
			removedResults = removed.toJson().get("results").getAsJsonObject();
		}
		
		results.add("addedresults", addedResults);
		results.add("removedresults", removedResults);
		results.add("head",head);
	}
	
	//TODO serialized according to the SPARQL 1.1 SE Notification JSON format (TBD)
	public String toString() {
		return results.toString();
	}
	
	public JsonObject toJson(){
		return results;
	}

	public SPARQLBindingsResults getAddedBindings() {
		JsonObject ret = new JsonObject();
		ret.add("results", results.get("addedresults"));
		ret.add("head", results.get("head"));
		return new SPARQLBindingsResults(ret);
	}

	public SPARQLBindingsResults getRemovedBindings() {
		JsonObject ret = new JsonObject();
		ret.add("results", results.get("removedresults"));
		ret.add("head", results.get("head"));
		return new SPARQLBindingsResults(ret);
	}
}
