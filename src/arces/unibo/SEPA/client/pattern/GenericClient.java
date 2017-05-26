/* This class implements a generic client of the SEPA Application Design Pattern (including the query primitive)
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

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

package arces.unibo.SEPA.client.pattern;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.QueryResponse;
import arces.unibo.SEPA.commons.response.Response;

public abstract class GenericClient extends Aggregator {	
	private static final Logger logger = LogManager.getLogger("GenericClient");
	
	public GenericClient(String jparFile) throws IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException {
		super(jparFile);	
	}
	
	public boolean update(String SPARQL_UPDATE,Bindings forced) {
		sparqlUpdate = SPARQL_UPDATE;
		return super.update(forced);
	 }
	
	public BindingsResults query(String SPARQL_QUERY,Bindings forced) {
		if (protocolClient == null) {
			 logger.fatal("Client is not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_QUERY,forced);

		logger.debug("SEPA","QUERY "+sparql);
		
		Response response = protocolClient.query(new QueryRequest(sparql));
		logger.debug(response.toString());
		 
		if (response.getClass().equals(ErrorResponse.class)) return null;
		
		return ((QueryResponse)response).getBindingsResults();
	}
	
	public String subscribe(String SPARQL_SUBSCRIBE,Bindings forced) throws IOException, URISyntaxException {	
		sparqlSubscribe = SPARQL_SUBSCRIBE;
		return super.subscribe(forced);
	}
	 
	public boolean unsubscribe() throws IOException, URISyntaxException {
		return super.unsubscribe();
	}
}
