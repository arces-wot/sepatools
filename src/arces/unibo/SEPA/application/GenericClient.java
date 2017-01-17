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
import arces.unibo.SEPA.client.SecureEventProtocol.NotificationHandler;
import arces.unibo.SEPA.commons.BindingsResults;
import arces.unibo.SEPA.commons.Bindings;

public class GenericClient extends Client {
	private String subID = null;
	private NotificationHandler handler;
	private String tag = "Generic client";
	
	public GenericClient(String url,int updatePort,int subscribePort,String path,NotificationHandler handler){
		super(url,updatePort,subscribePort,path);
		this.handler = handler;
	}
	
	public boolean update(String SPARQL_UPDATE,Bindings forced) {
		 if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		 }
		 
		String sparql = prefixes() + super.replaceBindings(SPARQL_UPDATE,forced).replace("\n", "").replace("\r", "");
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Update "+sparql);
		
		return protocolClient.update(sparql);
	 }
	
	public BindingsResults query(String SPARQL_QUERY,Bindings forced) {
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_QUERY,forced).replace("\n", "").replace("\r", "");

		Logger.log(VERBOSITY.DEBUG,"SEPA","QUERY "+sparql);
		
		return protocolClient.query(sparql);
	}
	
	public String subscribe(String SPARQL_SUBSCRIBE,Bindings forced) {	
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return null;
		 }
		
		if (subID != null) {
			 Logger.log(VERBOSITY.ERROR, tag, "Client is subscribed. First unsubscribe "+subID);
			 return null;
		 }
		
		String sparql = prefixes() + super.replaceBindings(SPARQL_SUBSCRIBE,forced).replace("\n", "").replace("\r", "");
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Subscribe "+sparql);
		
		subID = protocolClient.subscribe(sparql, handler);
		
		return subID;
	}
	 
	public boolean unsubscribe() {
		if (protocolClient == null) {
			 Logger.log(VERBOSITY.FATAL, tag, "Client not initialized");
			 return false;
		}
		
		if (subID == null) {
			 Logger.log(VERBOSITY.ERROR, tag, "Client is not subscribed");
			 return false;
		}
		
		Logger.log(VERBOSITY.DEBUG,"SEPA","Unsubscribe "+subID);
		
		boolean ret = protocolClient.unsubscribe(subID);
		
		if (ret) subID = null;
		
		return ret;
	}
}
