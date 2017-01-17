/* This class represents a generic request (i.e., QUERY, UPDATE, SUBSCRIBE, UNSUBSCRIBE)
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

/**
 * This class represents a generic request (i.e., QUERY, UPDATE, SUBSCRIBE, UNSUBSCRIBE)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class Request {
	private Integer token;
	private String sparql;
	
	public Request(Integer token,String sparql) {
		this.token = token;
		this.sparql = sparql;
	}
	
	public Integer getToken() {
		return token;
	}
	
	public String getSPARQL() {
		return sparql;
	}
}
