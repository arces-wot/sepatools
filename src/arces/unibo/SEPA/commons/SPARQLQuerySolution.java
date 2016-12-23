package arces.unibo.SEPA.commons;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class represents a single query solution of a SPARQL 1.1 Query
 * 
 * An example of the internal representation as JSON object follows:
 * 
 * {
 * 		"x" : 		{ 
 * 						"type": "bnode", 
 * 						"value": "r2" 
 * 					},
 *   	"hpage" : 	{	
 *   					"type": "uri", 
 *   					"value": "http://work.example.org/alice/" 
 *   				},
 *    	"blurb" : 	{
 *    					"datatype": "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral",
 *    					"type": "literal",
 *    					"value": "<p xmlns=\"http://www.w3.org/1999/xhtml\">My name is <b>alice</b></p>"
 *    				},
 *    	"name" : 	{ 
 *    					"type": "literal", 
 *    					"value": "Bob", 
 *    					"xml:lang": "en" 
 *    				}
 *    }
 *             
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class SPARQLQuerySolution {
	private JsonObject solution;

	public SPARQLQuerySolution(JsonObject solution) {
		this.solution = solution;
	}

	public SPARQLQuerySolution() {
		solution = new JsonObject();
	}

	public Set<String> getVariables() {
		Set<String> ret = new HashSet<String>();
		for (Entry<String,JsonElement> entry: solution.entrySet()) {
			ret.add(entry.getKey());
		}
		return ret;
	}
	
	public String getBindingValue(String variable){
		if(solution.get(variable) == null) return null;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("value").getAsString();
	}
	
	public String getDatatype(String variable){
		if(solution.get(variable) == null) return null;
		JsonObject json = solution.get(variable).getAsJsonObject();
		if (json.get("datatype") == null) return null;
		return json.get("datatype").getAsString();
	}
	
	public String getLanguage(String variable){
		if(solution.get(variable) == null) return null;
		JsonObject json = solution.get(variable).getAsJsonObject();
		if (json.get("xml:lang") == null) return null;
		return json.get("xml:lang").getAsString();
	}
	
	public boolean isLiteral(String variable){
		if(solution.get(variable) == null) return false;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("type").getAsString().equals("literal");
	}
	
	public boolean isURI(String variable){
		if(solution.get(variable) == null) return false;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("type").getAsString().equals("uri");
	}
	
	public boolean isBNode(String variable){
		if(solution.get(variable) == null) return false;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("type").getAsString().equals("bnode");
	}
	
	public void addBinding(String variable,RDFTerm value){
		solution.add(variable,value.toJson());
	}
	
	public boolean equals(SPARQLQuerySolution qs) {
		return this.solution.equals(qs.solution);
	}
	
	public JsonObject toJson() {
		return solution;
	}
	
	public String toString() {
		return solution.toString();
	}
}
