/* This class implements the processing of a SPARQL 1.1 UPDATE
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

import arces.unibo.SEPA.commons.UpdateRequest;
import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.Response;

public class UpdateProcessor {
	private Endpoint endpoint;
	private String tag ="Update Processor";

	public UpdateProcessor(Endpoint endpoint) {
		this.endpoint = endpoint;
	}
	
	public Response process(UpdateRequest req) {
		Logger.log(VERBOSITY.DEBUG, tag, "Process "+req.getSPARQL());
		
		Response res = endpoint.update(req);
		
		Logger.log(VERBOSITY.DEBUG, tag, "Response "+res.toString());
		return res;
	}
}
