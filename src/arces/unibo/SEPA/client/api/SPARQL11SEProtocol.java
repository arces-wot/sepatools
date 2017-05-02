/* This class implements the API of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol)
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

package arces.unibo.SEPA.client.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.SPARQL.SPARQL11Protocol;
import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.NotificationHandler;
import arces.unibo.SEPA.commons.response.QueryResponse;
import arces.unibo.SEPA.commons.response.Response;

public class SPARQL11SEProtocol extends SPARQL11Protocol {
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProtocol");
	
	private WebsocketEndpoint wsClient;
	
	private ClientProperties properties;
	
	public enum SUBSCRIPTION_STATE {SUBSCRIBED,UNSUBSCRIBED,BROKEN_SOCKET};
	
	public SPARQL11SEProtocol(ClientProperties properties) {
		super(properties);
		this.properties = properties;
		
		wsClient = new WebsocketEndpoint(properties.getWsScheme()+"://"+properties.getHost()+":"+properties.getWsPort()+properties.getSubscribePath());
	}

	public String toString() {
		return properties.toString();
	}
	
	public boolean update(String sparql) {
		Response response = super.update(new UpdateRequest(0,sparql));
		logger.debug(response.toString());
		return (!response.isError());
	}
	
	public BindingsResults query(String sparql) {
		Response response = super.query(new QueryRequest(0,sparql));
		logger.debug(response.toString());
		if (response.isError()) {
			logger.error(response);
			return null;
		}
		QueryResponse ret = (QueryResponse) response;
		return ret.getBindingsResults();
	}
	
	public boolean subscribe(String sparql,NotificationHandler handler) {
		logger.debug("Subscribe");
		return wsClient.subscribe(sparql,handler);
	}

	public boolean unsubscribe(String subID) {
		logger.debug("Unsubscribe");
		return wsClient.unsubscribe(subID);
	}
}
