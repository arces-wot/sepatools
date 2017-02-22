/* This class implements a SEPA producer
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

package arces.unibo.SEPA.application;

import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQL.Bindings;

public class Producer extends Client implements IProducer {
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	
	protected String tag = "SEPA PRODUCER";
	
	public Producer(String updateQuery,String url,int updatePort,int subscribePort,String path){
		super(url,updatePort,subscribePort,path);
		sparqlUpdate = updateQuery;
	}
	
	public Producer(ApplicationProfile appProfile,String updateID){
		super(appProfile);
		if (appProfile == null) {
			SEPALogger.log(VERBOSITY.FATAL, tag, "Cannot be initialized with UPDATE ID: "+updateID+" (application profile is null)");
			return;
		}
		if (appProfile.update(updateID) == null) {
			SEPALogger.log(VERBOSITY.FATAL, tag, "Cannot find UPDATE ID: "+updateID);
			return;
		}
		
		SPARQL_ID = updateID;
		
		sparqlUpdate = appProfile.update(updateID);
	}
	
	public boolean update(Bindings forcedBindings){	 
		 if (sparqlUpdate == null) {
			 SEPALogger.log(VERBOSITY.FATAL, tag, "SPARQL UPDATE not defined");
			 return false;
		 }
		 
		 if (protocolClient == null) {
			 SEPALogger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }

		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);
		 
		 SEPALogger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+ SPARQL_ID+" ==> "+sparql);
		 
		 return protocolClient.update(sparql);
	 }
}
