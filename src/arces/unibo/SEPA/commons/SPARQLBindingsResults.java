package arces.unibo.SEPA.commons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * This class represents the results of a SPARQL 1.1 Query
 * 
 * This conforms with the following:
 * - SPARQL 1.1 Query Results JSON Format https://www.w3.org/TR/2013/REC-sparql11-results-json-20130321/
 * 
 * It uses https://github.com/google/gso as internal representation of the results in JSON format
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class SPARQLBindingsResults {
	
	private JsonObject results;
	
	public SPARQLBindingsResults(JsonObject results) {
		this.results = results;
	}
	
	public SPARQLBindingsResults(Set<String> varSet,List<SPARQLQuerySolution> solutions) {
		results = new JsonObject();
		
		JsonObject vars = new JsonObject();
		JsonArray varArray = new JsonArray();
		if (varSet != null) 
			for (String var: varSet) {
				varArray.add(var);
			}
		vars.add("vars", varArray);
		results.add("head", vars);
		
		JsonArray bindingsArray = new JsonArray();
		if (solutions != null) 
			for (SPARQLQuerySolution solution : solutions) bindingsArray.add(solution.toJson());
		JsonObject bindings = new JsonObject();
		bindings.add("bindings", bindingsArray);
		results.add("results", bindings);
	}
	
	public SPARQLBindingsResults(SPARQLBindingsResults newBindings) {
		results = new JsonObject();
		
		JsonObject vars = new JsonObject();
		JsonArray varArray = new JsonArray();
		if (newBindings != null)
			for (String var: newBindings.getVariables()) {
				varArray.add(var);
			}
		vars.add("vars", varArray);
		results.add("head", vars);
		
		JsonArray bindingsArray = new JsonArray();
		if (newBindings != null)
			for (SPARQLQuerySolution solution : newBindings.getBindings()) bindingsArray.add(solution.toJson());
		JsonObject bindings = new JsonObject();
		bindings.add("bindings", bindingsArray);
		results.add("results", bindings);
	}

	public Set<String> getVariables() {
		Set<String> vars = new HashSet<String>();
		for (JsonElement var : results.get("head").getAsJsonObject().get("vars").getAsJsonArray()) vars.add(var.getAsString());
		return vars;
	}
	
	public List<SPARQLQuerySolution> getBindings() {
		List<SPARQLQuerySolution> list = new ArrayList<SPARQLQuerySolution>();
		for (JsonElement solution : results.get("results").getAsJsonObject().get("bindings").getAsJsonArray() ) {
			list.add(new SPARQLQuerySolution(solution.getAsJsonObject()));
		}
		return list;
	}
	
	public JsonObject toJson() {
		return results;
	}
	
	public String toString() {
		return results.toString();
	}

	public boolean isEmpty() {
		return (results.get("results").getAsJsonObject().get("bindings").getAsJsonArray().size() == 0);
	}

	public void add(SPARQLQuerySolution binding) {
		results.get("results").getAsJsonObject().get("bindings").getAsJsonArray().add(binding.toJson());
	}

	public boolean contains(SPARQLQuerySolution solution) {
		return results.get("results").getAsJsonObject().get("bindings").getAsJsonArray().contains(solution.toJson());
	}

	public void remove(SPARQLQuerySolution solution) {
		results.get("results").getAsJsonObject().get("bindings").getAsJsonArray().remove(solution.toJson());
	}

	public int size() {
		return results.get("results").getAsJsonObject().get("bindings").getAsJsonArray().size();
	}
}