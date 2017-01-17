/* This class represents a notification message
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

/**
 * This class represents a SPARQL Notification (see SPARQL 1.1 Notification Language)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Notification extends Response {
	private String spuid;
	private ARBindingsResults results;
	private Integer sequence;
	
	public Notification(String spuid,ARBindingsResults results,Integer sequence) {
		super(0,results.toJson());
		
		JsonPrimitive jsonSpuid = new JsonPrimitive(spuid);
		JsonPrimitive jsonSequence = new JsonPrimitive(sequence);
		super.toJson().add("spuid", jsonSpuid);
		super.toJson().add("sequence", jsonSequence);
		
		this.spuid = spuid;
		this.results = results;
		this.sequence = sequence;
	}
	
	public String getSPUID() {
		return spuid;
	}
	
	public String toString() {
		return super.toString();
	}

	public ARBindingsResults getARBindingsResults() {
		return results;
	}

	public Integer getSequence() {
		return sequence;
	}
}
