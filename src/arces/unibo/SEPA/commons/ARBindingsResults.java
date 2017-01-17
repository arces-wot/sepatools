/* This class represents the content of a SEPA notification
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
	
	public ARBindingsResults(BindingsResults added,BindingsResults removed) {
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

	public BindingsResults getAddedBindings() {
		JsonObject ret = new JsonObject();
		ret.add("results", results.get("addedresults"));
		ret.add("head", results.get("head"));
		return new BindingsResults(ret);
	}

	public BindingsResults getRemovedBindings() {
		JsonObject ret = new JsonObject();
		ret.add("results", results.get("removedresults"));
		ret.add("head", results.get("head"));
		return new BindingsResults(ret);
	}
}
