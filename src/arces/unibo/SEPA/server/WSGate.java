/* This class implements the SPARQL Secure Event (SE) 1.1 Protocol 
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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import arces.unibo.SEPA.beans.SEPABeans;
import arces.unibo.SEPA.beans.WebSocketGateMBean;

import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.Ping;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;

import arces.unibo.SEPA.server.RequestResponseHandler.ResponseAndNotificationListener;

/**
 * This class implements the SPARQL 1.1 Secure Event (SE) Protocol to handle Subscribe and Unsubscribe primitives
 * 
 * This is based on Websockets Protocol RFC 6455
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class WSGate extends WebSocketApplication implements WebSocketGateMBean {
	
	protected Scheduler scheduler;
	
	private int wsPort = 9000;
	protected int keepAlivePeriod = 5000;
	
	private Logger logger = LogManager.getLogger("WSGate");	
	protected static String mBeanName = "arces.unibo.SEPA.server:type=WSGate";
	
	protected HashMap<WebSocket,SEPAResponseListener> activeSockets = new HashMap<WebSocket,SEPAResponseListener>();
		
	public class SEPAResponseListener implements ResponseAndNotificationListener {
		private WebSocket socket;	
		private HashSet<String> spuIds = new HashSet<String>();
		
		public int activeSubscriptions() {
			return spuIds.size();
		}
		
		public void unsubscribeAll() {
			synchronized(spuIds) {
				Iterator<String> it = spuIds.iterator();
				
				while(it.hasNext()) {
					Integer token = scheduler.getToken();
					logger.debug(">> Scheduling UNSUBSCRIBE request #"+token);
					scheduler.addRequest(new UnsubscribeRequest(token,it.next()),this);		
				}
			}
		}
		
		@Override
		public void notify(Response response) {		
			if (response.getClass().equals(SubscribeResponse.class)) {
				logger.debug("<< SUBSCRIBE response #"+response.getToken());
				
				synchronized(spuIds) {
					spuIds.add(((SubscribeResponse)response).getSPUID());
				}
			
			}else if(response.getClass().equals(UnsubscribeResponse.class)) {
				logger.debug("<< UNSUBSCRIBE response #"+response.getToken()+" ");
				
				synchronized(spuIds) {
					spuIds.remove(((UnsubscribeResponse)response).getSPUID());
				
					synchronized(activeSockets) {
						if (spuIds.isEmpty()) activeSockets.remove(socket);
					}
				}
			}
			
			//Send response to client
			if (socket != null) if (socket.isConnected()) socket.send(response.toString());	
			
			//Release token
			if (!response.getClass().equals(Notification.class)) scheduler.releaseToken(response.getToken());
		}
		
		public Set<String> getSPUIDs() {
			return spuIds;
		}
		
		public SEPAResponseListener(WebSocket socket) {
			this.socket = socket;
		}
	}
		
	public WSGate(Properties properties,Scheduler scheduler) {
		if (scheduler == null) logger.error("Scheduler is null");
		this.scheduler = scheduler;
		
		if (properties == null) logger.error("Properties are null");
		else {
			wsPort = Integer.parseInt(properties.getProperty("wsPort", "9000"));
			keepAlivePeriod =  Integer.parseInt(properties.getProperty("keepAlivePeriod", "5000"));
		}
		
		SEPABeans.registerMBean(this,mBeanName);
	}
	
	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		logger.debug("onClose: "+socket.toString());
		
		if (keepAlivePeriod == 0) activeSockets.get(socket).unsubscribeAll();//unsubscribeAllSPUs(socket);
	}

	@Override
	public void onConnect(WebSocket socket) {
		logger.debug("onConnect: "+socket.toString());
		SEPAResponseListener listener = new SEPAResponseListener(socket);
		
		synchronized(activeSockets) {
			activeSockets.put(socket, listener);
		}
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		Integer token = scheduler.getToken();
		
		Request request = parseRequest(token,text);
		
		if(request == null) {
			logger.debug("Not supported request: "+text);
			
			ErrorResponse response = new ErrorResponse(token,"Not supported request: "+text);
			
			socket.send(response.toString());
			
			scheduler.releaseToken(token);
			
			return;
		}
		
		synchronized(activeSockets) {
			logger.debug(">> Scheduling request: "+request.toString());
			scheduler.addRequest(request,activeSockets.get(socket));	
		}
	}
	
	/* SPARQL 1.1 Subscribe language 
	 * 
	 * {"subscribe":"SPARQL Query 1.1", "authorization": "JWT"}
	 * 
	 * {"subscribe":"SPUID", "authorization": "JWT"}
	 * 
	 * In not secure connections (ws), authorization key can be missing
	 * */
	protected Request parseRequest(Integer token,String request) {
		JsonObject req;
		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			return null;
		}
		
		if (req.get("subscribe") != null) return new SubscribeRequest(token,req.get("subscribe").getAsString());
		if (req.get("unsubscribe") != null) return new UnsubscribeRequest(token,req.get("unsubscribe").getAsString());	
		
		return null;
	}
	
	public int getPort() {
		return wsPort;
	}
	
	public boolean start(){
		final HttpServer server = HttpServer.createSimpleServer("/var/www", wsPort);

        // Register the WebSockets add on with the HttpServer
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());

        // register the application
        WebSocketEngine.getEngine().register("", "/sparql", this);

        try {
			server.start();
		} catch (IOException e) {
			logger.info("Failed to start WebSocket gate on port "+wsPort+ " "+e.getMessage());
			return false;
		}
		
		logger.info("Started on port "+wsPort);

		if (keepAlivePeriod > 0) {
			new KeepAlive().start();
		}
		return true;
	}
	
	public class KeepAlive extends Thread {
		public void run() {
			while(true) {
				try {
					Thread.sleep(keepAlivePeriod);
				} catch (InterruptedException e) {
					return;
				}
				
				//Send heart beat on each active socket to detect broken sockets				
				synchronized(activeSockets) {
					for(WebSocket socket : activeSockets.keySet()) {	
						//Send ping only on sockets with active subscriptions
						if (activeSockets.get(socket).activeSubscriptions() == 0) continue;
						
						if (socket.isConnected()) {
							Ping ping = new Ping();
							socket.send(ping.toString());
						}
						else {
							activeSockets.get(socket).unsubscribeAll();
						}
					}
				}					
			}
		}
	}

	@Override
	public int getActiveWebSockets() {
		return this.activeSockets.size();
	}

}
