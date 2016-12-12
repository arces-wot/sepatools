package arces.unibo.SUBEngine;

import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import arces.unibo.SUBEngine.SPARQLQuerySolution.RDFTerm;
import arces.unibo.SUBEngine.SPARQLQuerySolution.RDFTermBNode;
import arces.unibo.SUBEngine.SPARQLQuerySolution.RDFTermLiteral;
import arces.unibo.SUBEngine.SPARQLQuerySolution.RDFTermURI;

/**
 * This class represents the results of a SPARQL 1.1 Query
 * 
 * This conforms with the following:
 * - SPARQL 1.1 Query Results JSON Format https://www.w3.org/TR/2013/REC-sparql11-results-json-20130321/
 * - SPARQL Query Results XML Format (Second Edition) https://www.w3.org/TR/2013/REC-rdf-sparql-XMLres-20130321/
 * 
 * It uses https://github.com/google/gso to serialize the results in JSON format
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class SPARQLBindingsResults {
	
	private String results = null;
	
	private Set<String> variables = null;
	private Set<SPARQLQuerySolution> bindings = null;
	
	public SPARQLBindingsResults(String results){
		this.results = results;
	}
	
	public SPARQLBindingsResults() {}
	
	public Set<String> getVariables() {
		return variables;	
	}
	
	public Set<SPARQLQuerySolution> getBindings() {
		return bindings;
	}
	
	public void merge(SPARQLBindingsResults res1,SPARQLBindingsResults res2) {
		bindings.addAll(res1.getBindings());
		bindings.addAll(res2.getBindings());
		variables.addAll(res1.getVariables());
		variables.addAll(res2.getVariables());
	}
	
	public String toString() {
		if (results != null) return results;
		return toJson();
	}
	
	private String toJson() {
		JsonObject results = new JsonObject();
		
		JsonArray varsArray = new JsonArray();
		JsonObject vars = new JsonObject();
		JsonArray bindingsArray = new JsonArray();
		JsonObject bindings = new JsonObject();
				
		results.add("head", vars);
		results.add("results", bindings);
		vars.add("vars", varsArray);
		bindings.add("bindings", bindingsArray);
		
		for(String var: this.variables){
			varsArray.add(var);
		}
		
		for(SPARQLQuerySolution bind: this.bindings) {
			for (String variable : bind.getVariables()){
				JsonObject bindingsEntry = new JsonObject();
				RDFTerm value = bind.getBindingValue(variable);
				if (value != null) {
					bindingsEntry.add("value", new JsonPrimitive(value.getValue()));
					if (value.getClass().equals(RDFTermURI.class)){
						bindingsEntry.add("type", new JsonPrimitive("uri"));
					}
					else if (value.getClass().equals(RDFTermBNode.class)) {
						bindingsEntry.add("type", new JsonPrimitive("bnode"));
					}
					else {
						bindingsEntry.add("type", new JsonPrimitive("literal"));
						RDFTermLiteral literalValue = (RDFTermLiteral) value;
						if (literalValue.getLanguageTag() != null) {
							bindingsEntry.add("xml:lang", new JsonPrimitive(literalValue.getLanguageTag()));
						}
						else if (literalValue.getDatatype() != null){
							bindingsEntry.add("datatype", new JsonPrimitive(literalValue.getDatatype()));
						}
					}
				}
			}
		}
		
		return results.toString();
	}
}