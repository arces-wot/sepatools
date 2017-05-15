/* This class represents a generic abstract response
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

/**
 * This class represents the response to a generic request.
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class Response {
	protected JsonObject json;
	private int token = -1;

	public Response(Integer token) {
		this.token = token;
		json = new JsonObject();
	}
	
	public Response() {
		json = new JsonObject();
	}
	
	@Override
	public String toString() {
		return json.toString();
	}
	
	public int getToken() {
		return token;
	}
	
	public JsonObject getAsJsonObject(){
		return json;
	}
}
