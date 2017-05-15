/* This class represents the response to a SPARQL 1.1 query
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package arces.unibo.SEPA.commons.response;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.commons.SPARQL.BindingsResults;

/**
 * This class represents the results of a SPARQL 1.1 Query
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class QueryResponse extends Response {
	
	public QueryResponse(Integer token, JsonObject body) {
		super(token);
		
		if (body != null) json.add("body", body);
		json.add("code", new JsonPrimitive(200));
	}
	
	public QueryResponse(JsonObject body) {
		super();
		
		if (body != null) json.add("body", body);
		json.add("code", new JsonPrimitive(200));
	}
	
	public BindingsResults getBindingsResults() {
		if (json.get("body") != null) return new BindingsResults(json.get("body").getAsJsonObject());
		return null;
	}
}
