/* This class abstracts the aggregator client of the SEPA Application Design Pattern
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
import arces.unibo.SEPA.commons.SPARQL.Bindings;

public abstract class Aggregator extends Consumer implements IAggregator {
	protected String sparqlUpdate = "INSERT { ?subject ?predicate ?object }";
	protected String updateID = "";
	protected String tag = "SEPA AGGREGATOR";
	
	public Aggregator(String url,int updatePort,int subscribePort,String path,String subscribe,String update) {
		super(url,updatePort,subscribePort,path,subscribe);
		sparqlUpdate = update;
	}
	
	public Aggregator(ApplicationProfile appProfile,String subscribeID,String updateID){
		super(appProfile,subscribeID);
		
		if (appProfile == null){
			Logger.log(VERBOSITY.FATAL,tag,"Cannot be initialized with UPDATE ID " +updateID+" (application profile is null)");
			return;	
		}
		if (appProfile.update(updateID) == null) {
			Logger.log(VERBOSITY.FATAL,tag,"UPDATE ID " +updateID+" not found");
			return;
		}
		
		sparqlUpdate = appProfile.update(updateID);
		this.updateID = updateID;
	} 
		
	public boolean update(Bindings forcedBindings){
		 
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.ERROR,tag,"UPDATE " +updateID+" FAILED because client has not been inizialized");
			 return false;
		 }
		 
		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);
		 
		 Logger.log(VERBOSITY.DEBUG,tag,"<UPDATE> "+updateID+" ==> "+sparql);
		 
		 return protocolClient.update(sparql);
	 }
}
