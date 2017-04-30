/* This class implements the processing of a SPARQL 1.1 QUERY
    Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

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

package arces.unibo.SEPA.server;

import arces.unibo.SEPA.commons.SPARQL.Endpoint;
import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.response.Response;

public class QueryProcessor {
	private Endpoint endpoint;
	
	public QueryProcessor(Endpoint endpoint) {
		this.endpoint = endpoint;
	}
	
	public Response process(QueryRequest req) {		
		return endpoint.query(req);
	}
}
