/* This class implements a generic client of the SEPA Application Design Pattern (including the query primitive)
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
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;

public abstract class GenericClient extends Aggregator {	
	private String tag = "Generic client";
	
	public GenericClient(ApplicationProfile appProfile,String subscribeID,String updateID) {
		super(appProfile,subscribeID,updateID);
	}
	public GenericClient(String url,int updatePort,int subscribePort,String path){
		super(url,updatePort,subscribePort,path,"","");	
	}
	
	public boolean update(String SPARQL_UPDATE,Bindings forced) {
		sparqlUpdate = SPARQL_UPDATE;
		return super.update(forced);
	 }
	
	public BindingsResults query(String SPARQL_QUERY,Bindings forced) {
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_QUERY,forced);

		Logger.log(VERBOSITY.DEBUG,"SEPA","QUERY "+sparql);
		
		return protocolClient.query(sparql);
	}
	
	public String subscribe(String SPARQL_SUBSCRIBE,Bindings forced) {	
		sparqlSubscribe = SPARQL_SUBSCRIBE;
		return super.subscribe(forced);
	}
	 
	public boolean unsubscribe() {
		return super.unsubscribe();
	}
}
