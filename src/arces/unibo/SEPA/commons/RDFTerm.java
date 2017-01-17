/* This class represents a RDF term (URI, literal or blank node)
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

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;

public abstract class RDFTerm {
	protected JsonObject json = null;
	
	public String getValue() {
		return json.get("value").getAsString();
	}
	
	public boolean equals(RDFTerm t) {
		return this.json.equals(t.toJson());
	}

	public JsonObject toJson() {
		return json;
	}
	
	public RDFTerm(String value) {
		json = new JsonObject();
		json.add("value", new JsonPrimitive(value));
	}
	
	public String toString() {
		return json.toString();
	}
}
