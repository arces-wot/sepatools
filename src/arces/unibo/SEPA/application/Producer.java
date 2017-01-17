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

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.Bindings;

public class Producer extends Client implements IProducer {
	private String SPARQL_UPDATE = null;
	private String SPARQL_ID = "";
	private String tag = "SEPA PRODUCER";
	
	public Producer(String updateQuery,String url,int updatePort,int subscribePort,String path){
		super(url,updatePort,subscribePort,path);
		SPARQL_UPDATE = updateQuery.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
	}
	
	public Producer(ApplicationProfile appProfile,String updateID){
		super(appProfile);
		if (appProfile == null) {
			Logger.log(VERBOSITY.FATAL, tag, "Cannot be initialized with UPDATE ID: "+updateID+" (application profile is null)");
			return;
		}
		if (appProfile.update(updateID) == null) {
			Logger.log(VERBOSITY.FATAL, tag, "Cannot find UPDATE ID: "+updateID);
			return;
		}
		
		SPARQL_ID = updateID;
		
		SPARQL_UPDATE = appProfile.update(updateID).replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
	}
	
	public boolean update(Bindings forcedBindings){	 
		 if (SPARQL_UPDATE == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "SPARQL UPDATE not defined");
			 return false;
		 }
		 
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }

		 String sparql = prefixes() + replaceBindings(SPARQL_UPDATE,forcedBindings);
		 
		 Logger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+ SPARQL_ID+" ==> "+sparql);
		 
		 return protocolClient.update(sparql);
	 }
}
