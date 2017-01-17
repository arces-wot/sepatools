/* This class represents a UPDATE response
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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This class represents the response of a SPARQL 1.1 Update
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class UpdateResponse extends Response {

	public UpdateResponse(Integer token, String message) {
		super(token,new JsonObject());		
		json.add("message", new JsonPrimitive(message));
	}
}