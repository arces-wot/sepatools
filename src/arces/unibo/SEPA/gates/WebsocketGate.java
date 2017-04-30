/* This class implements the SPARQL 1.1 SE Protocol specific subscription part
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

package arces.unibo.SEPA.gates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketListener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.Ping;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;
import arces.unibo.SEPA.security.AuthorizationManager;
import arces.unibo.SEPA.security.SecurityManager;
import arces.unibo.SEPA.server.EngineProperties;
import arces.unibo.SEPA.server.Scheduler;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseAndNotificationListener;

/* SPARQL 1.1 Subscribe language 
 * 
 * {"subscribe":"SPARQL Query 1.1", "authorization": "JWT"}
 * 
 * {"unsubscribe":"SPUID", "authorization": "JWT"}
 * 
 * In not secure connections (ws), authorization key can be missing
 * */
public class WebsocketGate extends WebSocketApplication {
	protected Logger logger = LogManager.getLogger("WebsocketGate");
	protected EngineProperties properties;
	protected Scheduler scheduler;
	
	//Security context and manager
	private SecurityManager sManager = new SecurityManager();
	private AuthorizationManager am = new AuthorizationManager();
	private ArrayList<WebSocket> secureSockets = new ArrayList<WebSocket>();
	
	//Collection of active sockets
	protected HashMap<WebSocket,SEPAResponseListener> activeSockets = new HashMap<WebSocket,SEPAResponseListener>();
	
	@Override
	public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket requestPacket,WebSocketListener... listeners) {
	    logger.debug("@createSocket");
	    logger.debug("Protocol : " + requestPacket.getProtocol().getProtocolString());
	    logger.debug("Local port : " + requestPacket.getLocalPort());
	    WebSocket ret = super.createSocket(handler, requestPacket, listeners);
	    if (requestPacket.getLocalPort() == properties.getWssPort()) {
	    	//Add the websocket to the secure set 
	    	secureSockets.add(ret);
	    }
		return ret;
	}

	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		logger.debug("@onClose");
		super.onClose(socket, frame);
		
		secureSockets.remove(socket);
		if (properties.getKeepAlivePeriod() == 0) activeSockets.get(socket).unsubscribeAll();
	}

	@Override
	public void onConnect(WebSocket socket) {
		logger.debug("@onConnect");
		super.onConnect(socket);
		
		if (secureSockets.contains(socket)) logger.debug("Secure socket");
		SEPAResponseListener listener = new SEPAResponseListener(socket);
		
		synchronized(activeSockets) {
			activeSockets.put(socket, listener);
		}
	    
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		logger.debug("@onMessage");
		super.onMessage(socket, text);
		
		if (secureSockets.contains(socket)) logger.debug("Secure socket");
		Integer token = scheduler.getToken();
		
		Request request = parseRequest(token,text);
		
		if(request == null) {
			logger.debug("Not supported request: "+text);
			
			ErrorResponse response = new ErrorResponse(token,"Not supported request: "+text,400);
			
			socket.send(response.toString());
			
			scheduler.releaseToken(token);
			
			return;
		}
		
		//JWT Validation
		if (secureSockets.contains(socket)) {
			if (!validateToken(text)) {
				//Not authorized
				logger.warn("NOT AUTHORIZED");
				JsonObject error = new JsonObject();
				error.add("error", new JsonPrimitive("Authorization missing or token not valid"));
				socket.send(error.toString());
			}
		}
		else {
			synchronized(activeSockets) {
				logger.debug(">> Scheduling request: "+request.toString());
				scheduler.addRequest(request,activeSockets.get(socket));	
			}
		}
		
	}
	
	private Request parseRequest(Integer token,String request) {
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

	private boolean validateToken(String request) {
		JsonObject req;
		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			return false;
		}
		
		if (req.get("authorization") == null) return false;
		
		//Token validation
		 return am.validateToken(req.get("authorization").getAsString()).get("valid").getAsBoolean();
	}
	
	public WebsocketGate(EngineProperties properties,Scheduler scheduler) {
		if (scheduler == null) {
			logger.fatal("Scheduler is null");
			System.exit(1);
		}
		
		if (properties == null) {
			logger.fatal("Properties are null");
			System.exit(1);
		}
		
		this.properties = properties;
		this.scheduler = scheduler;
	}
	
	public boolean start(){
		
		//Create an HTTP server to which attach the websocket
		final HttpServer server = HttpServer.createSimpleServer(null, properties.getWsPort());
		final HttpServer secureServer = HttpServer.createSimpleServer(null, properties.getWssPort());
		
		// Register the WebSockets add on with the HttpServer
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());
        secureServer.getListener("grizzly").registerAddOn(new WebSocketAddOn());
		
        // Security settings
        secureServer.getListener("grizzly").setSSLEngineConfig(sManager.getWssConfigurator());
		secureServer.getListener("grizzly").setSecure(true);
		
        // register the application
        WebSocketEngine.getEngine().register("", "/sparql", this);
		
        //Start the server
        try {
			server.start();
		} catch (IOException e) {
			logger.fatal("Failed to start WebSocket gate on port "+properties.getWsPort()+ " "+e.getMessage());
			System.exit(1);
		}
        try {
			secureServer.start();
		} catch (IOException e) {
			logger.fatal("Failed to start Secure WebSocket gate on port "+properties.getWssPort()+ " "+e.getMessage());
			System.exit(1);
		}
        
		logger.info("Started on port "+properties.getWsPort());
		logger.info("Started on secure port "+properties.getWssPort());
		
		//Start the keep alive thread
		if (properties.getKeepAlivePeriod() > 0) new KeepAlive().start();
		
		return true;
	}
	
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
	
	public class KeepAlive extends Thread {
		public void run() {
			while(true) {
				try {
					Thread.sleep(properties.getKeepAlivePeriod());
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
		
	
}
