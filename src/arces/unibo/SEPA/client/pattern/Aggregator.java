/* This class abstracts the aggregator client of the SEPA Application Design Pattern
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Response;

public abstract class Aggregator extends Consumer implements IAggregator {
	protected String sparqlUpdate = "INSERT { ?subject ?predicate ?object }";
	protected String updateID = "";
	
	private static final Logger logger = LogManager.getLogger("Aggregator");
	
	public Aggregator(String url,int updatePort,int subscribePort,String path,String subscribe,String update) {
		super(url,updatePort,subscribePort,path,subscribe);
		sparqlUpdate = update;
	}
	
	public Aggregator(ApplicationProfile appProfile,String subscribeID,String updateID){
		super(appProfile,subscribeID);
		
		if (appProfile == null){
			logger.fatal("Cannot be initialized with UPDATE ID " +updateID+" (application profile is null)");
			return;	
		}
		if (appProfile.update(updateID) == null) {
			logger.fatal("UPDATE ID " +updateID+" not found");
			return;
		}
		
		sparqlUpdate = appProfile.update(updateID);
		this.updateID = updateID;
	} 
		
	public boolean update(Bindings forcedBindings){
		 
		 if (protocolClient == null) {
			 logger.fatal("UPDATE " +updateID+" FAILED because client has not been inizialized");
			 return false;
		 }
		 
		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);
		 
		 logger.debug("<UPDATE> "+updateID+" ==> "+sparql);
		 
		 Response response = protocolClient.update(new UpdateRequest(sparql));
		 logger.debug(response.toString());
		 
		 return !(response.getClass().equals(ErrorResponse.class));
	 }
}
