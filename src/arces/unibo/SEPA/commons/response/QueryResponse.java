/* This class represents a query response
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

package arces.unibo.SEPA.commons.response;

import com.google.gson.JsonParser;
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
	
	public QueryResponse(Integer token, String body) {
		super(token);
		
		json.add("body", new JsonParser().parse(body).getAsJsonObject());
		json.add("code", new JsonPrimitive(200));
	}
	
	public BindingsResults getBindingsResults() {
		return new BindingsResults(json.get("body").getAsJsonObject());
	}
}
