/* This class represents the results of a SPARQL 1.1 Query
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

public class BindingsResults {
	
	private JsonObject results;
	
	public BindingsResults(JsonObject results) {
		this.results = results;
	}
	
	public BindingsResults(Set<String> varSet,List<Bindings> solutions) {
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
			for (Bindings solution : solutions) bindingsArray.add(solution.toJson());
		JsonObject bindings = new JsonObject();
		bindings.add("bindings", bindingsArray);
		results.add("results", bindings);
	}
	
	public BindingsResults(BindingsResults newBindings) {
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
			for (Bindings solution : newBindings.getBindings()) bindingsArray.add(solution.toJson());
		JsonObject bindings = new JsonObject();
		bindings.add("bindings", bindingsArray);
		results.add("results", bindings);
	}

	public Set<String> getVariables() {
		Set<String> vars = new HashSet<String>();
		JsonArray variables = getVariablesArray();
		if (variables == null) return vars;
		
		for (JsonElement var : variables) vars.add(var.getAsString());
		return vars;
	}
	
	public List<Bindings> getBindings() {
		List<Bindings> list = new ArrayList<Bindings>();
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return list;
		
		for (JsonElement solution : bindings) {
			list.add(new Bindings(solution.getAsJsonObject()));
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
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return true;
		
		return (bindings.size() == 0);
	}

	public void add(Bindings binding) {
		if (binding == null) return;
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return;
		
		bindings.add(binding.toJson());
	}

	public boolean contains(Bindings solution) {
		if (solution == null) return false;
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return false;
		
		return bindings.contains(solution.toJson());
	}

	public void remove(Bindings solution) {
		if (solution == null) return;
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return;
		
		bindings.remove(solution.toJson());
	}

	public int size() {
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return 0;
		return bindings.size();
	}
	
	private JsonArray getBindingsArray() {
		JsonElement varArray;
		if (results == null) return null;
		if((varArray=results.get("results")) == null) return null;
		if((varArray=varArray.getAsJsonObject().get("bindings"))==null) return null;
		
		return varArray.getAsJsonArray();	
	}
	
	private JsonArray getVariablesArray() {
		JsonElement varArray;
		if (results == null) return null;
		if ((varArray = results.get("head")) == null) return null;
		if ((varArray = varArray.getAsJsonObject().get("vars")) == null) return null;
		
		return varArray.getAsJsonArray();	
	}
}