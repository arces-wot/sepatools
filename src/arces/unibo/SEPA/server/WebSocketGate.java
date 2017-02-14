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
import java.util.Properties;
import java.util.Set;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import arces.unibo.SEPA.commons.ErrorResponse;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.PingResponse;
import arces.unibo.SEPA.commons.Request;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.SubscribeResponse;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeResponse;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseListener;

/**
 * This class implements the SPARQL 1.1 Secure Event (SE) Protocol to handle Subscribe and Unsubscribe primitives
 * 
 * This is based on Websockets Protocol RFC 6455
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class WebSocketGate extends WebSocketApplication {//implements ResponseListener {
	private String tag ="WebSocketGate";
	
	private Scheduler scheduler;
	
	private int wsPort = 9000;
	private int keepAlivePeriod = 5000;
	
	private HashMap<WebSocket,SEPAResponseListener> activeSockets = new HashMap<WebSocket,SEPAResponseListener>();
	
	// logging
	Logger logger = LogManager.getRootLogger();
	
	public class SEPAResponseListener implements ResponseListener {
		private WebSocket socket;	
		private HashSet<String> spuIds = new HashSet<String>();
		
		@Override
		public void notifyResponse(Response response) {		
			if (response.getClass().equals(SubscribeResponse.class)) {
				logger.debug("SUBSCRIBE response #"+response.getToken());
				
				spuIds.add(((SubscribeResponse)response).getSPUID());
			
			}else if(response.getClass().equals(UnsubscribeResponse.class)) {
				logger.debug("UNSUBSCRIBE response #"+response.getToken()+" ");
				
				spuIds.remove(((UnsubscribeResponse)response).getSPUID());
				
				synchronized(activeSockets) {
					if (spuIds.isEmpty()) activeSockets.remove(socket);
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
	
	public WebSocketGate(Properties properties,Scheduler scheduler){
		if (scheduler == null) logger.error("Scheduler is null");
		this.scheduler = scheduler;
		
		if (properties == null) logger.error("Properties are null");
		else {
			wsPort = Integer.parseInt(properties.getProperty("wsPort", "9000"));
			keepAlivePeriod =  Integer.parseInt(properties.getProperty("keepAlivePeriod", "5000"));
		}
	}
	
	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		logger.debug("onClose: "+socket.toString());
		
		if (keepAlivePeriod == 0) unsubscribeAllSPUs(socket);
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
			scheduler.addRequest(request,activeSockets.get(socket));	
		}
	}
	
	//TODO SPARQL 1.1 Subscribe language
	private Request parseRequest(Integer token,String request) {
		if (request.trim().startsWith("subscribe=")) return new SubscribeRequest(token,request.substring(request.trim().indexOf("=")+1));
		if (request.trim().startsWith("unsubscribe=")) return new UnsubscribeRequest(token,request.substring(request.trim().indexOf("=")+1));
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
	
	private synchronized void unsubscribeAllSPUs(WebSocket socket) {
		for(String spuid : activeSockets.get(socket).getSPUIDs()) {
			Integer token = scheduler.getToken();
			logger.debug("UNSUBSCRIBE request #"+token);
			scheduler.addRequest(new UnsubscribeRequest(token,spuid),activeSockets.get(socket));		
		}
	}
	
	public class KeepAlive extends Thread {//implements ResponseListener{
		public void run() {
			while(true) {
				try {
					Thread.sleep(keepAlivePeriod);
				} catch (InterruptedException e) {
					return;
				}
				
				//Send heart beat on each active socket to detect broken sockets
				HashSet<WebSocket> brokenSockets = new HashSet<WebSocket>();
				
				synchronized(activeSockets) {
					for(WebSocket socket : activeSockets.keySet()) {	
						
						if (socket.isConnected()) {
							PingResponse ping = new PingResponse();
							socket.send(ping.toString());
						}
						else brokenSockets.add(socket);
					}
				}
				
				//Send a UNSUBSCRIBE request to all SPUs belonging to broken sockets
				for (WebSocket socket : brokenSockets) {
					unsubscribeAllSPUs(socket);
				}
					
			}
		}
	}
}
