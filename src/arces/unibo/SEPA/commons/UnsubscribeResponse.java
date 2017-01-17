/* This class represents a UNSUBSCRIBE response
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

public class UnsubscribeResponse extends Response {
	
	public UnsubscribeResponse(Integer token, String SPUID) {
		super(token, new JsonObject());
		
		JsonPrimitive jsonSpuid = new JsonPrimitive(SPUID);
		json.add("spuid", jsonSpuid);
	}
	
	public String getSPUID() {
		return super.toJson().get("spuid").getAsString();
	}

}
