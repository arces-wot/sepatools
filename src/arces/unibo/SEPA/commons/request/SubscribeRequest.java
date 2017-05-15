/* This class represents a subscribe request
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
package arces.unibo.SEPA.commons.request;

import arces.unibo.SEPA.commons.request.QueryRequest;

/**
 * This class represents the request of performing a SPARQL 1.1 Subscribe
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class SubscribeRequest extends QueryRequest {
	private String alias = null;
	
	public SubscribeRequest(String sparql) {
		super(sparql);
	}

	public SubscribeRequest(String sparql,String alias) {
		super(sparql);
		this.alias = alias;
	}

	public SubscribeRequest(Integer token, String sparql, String alias) {
		super(token, sparql);
		this.alias = alias;
	}

	public SubscribeRequest(Integer token, String sparql) {
		super(token, sparql);
	}

	public String toString() {
		String str = "SUBSCRIBE";
		if (token != -1) str += " #"+token;
		if (alias != null) str += "("+alias+")";
		str += " "+sparql;

		return str;
	}
	
	/**
	 * This method returns the alias of the subscription. 
	 * 
	 * @return The subscription alias or <i>null</i> is not present
	* */
	public String getAlias() {
		return alias;
	}
}
