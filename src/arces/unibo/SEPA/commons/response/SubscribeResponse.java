/* This class represents a SUBSCRIBE response
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

import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.commons.response.Response;

/**
 * This class represents the response to a SPARQL 1.1 Subscribe (see SPARQL 1.1 Subscription Language)
 *
 * The JSON serialization is the following:
 *
 * {"subscribed" : "SPUID"}
 *
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 * */

public class SubscribeResponse extends Response {

	public SubscribeResponse(Integer token,String spuid) {
		super(token);

		if (spuid != null) json.add("subscribed",new JsonPrimitive(spuid));
	}
	
	public SubscribeResponse(Integer token,String spuid,String alias) {
		super(token);

		if (spuid != null) json.add("subscribed",new JsonPrimitive(spuid));
		if (alias != null) json.add("alias",new JsonPrimitive(alias));
	}

	public SubscribeResponse(String spuid) {
		super();

		if (spuid != null) json.add("subscribed",new JsonPrimitive(spuid));
	}
	
	public SubscribeResponse(String spuid,String alias) {
		super();

		if (spuid != null) json.add("subscribed",new JsonPrimitive(spuid));
		if (alias != null) json.add("alias",new JsonPrimitive(alias));
	}
	
	public String getSpuid() {
		if (json.get("subscribed") == null) return "";
		return json.get("subscribed").getAsString();
	}
	
	public String getAlias() {
		if (json.get("alias") == null) return "";
		return json.get("alias").getAsString();
	}
}
