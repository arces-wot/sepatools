/* This class represents an error response message
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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class PingResponse {
	protected JsonObject json;
	
	public PingResponse() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String timestamp = sdf.format(date);
		json = new JsonObject();
		JsonArray array = new JsonArray();
		JsonObject time = new JsonObject();
		time.add("timestamp", new JsonPrimitive(timestamp));
		array.add(time);
		json.add("ping", array );
	}
	
	public String toString() {
		return json.toString();
	}
	
	public JsonObject toJson(){
		return json;
	}
}
