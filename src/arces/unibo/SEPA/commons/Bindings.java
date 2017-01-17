/* This class represents a query solution of a SPARQL 1.1 Query
Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.commons;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class represents a query solution of a SPARQL 1.1 Query
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

public class Bindings {
	private JsonObject solution;

	public Bindings(JsonObject solution) {
		this.solution = solution;
	}

	public Bindings() {
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
	
	public boolean equals(Bindings qs) {
		return this.solution.equals(qs.solution);
	}
	
	public JsonObject toJson() {
		return solution;
	}
	
	public String toString() {
		return solution.toString();
	}
}
