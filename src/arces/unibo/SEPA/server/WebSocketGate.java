package arces.unibo.SEPA.server;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.Notification;
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

public class WebSocketGate extends WebSocketApplication implements ResponseListener {
	private String tag ="WebSocketGate";
	
	private TokenHandler tokenHandler;
	private RequestResponseHandler requestHandler;
	
	private int wsPort = 7777;
	private int keepAlivePeriod = 5000;
	
	private HashMap<WebSocket,SEPAResponseListener> activeSockets = new HashMap<WebSocket,SEPAResponseListener>();
	
	public class SEPAResponseListener implements ResponseListener {
		private WebSocket socket;
		
		private HashSet<String> spuIds = new HashSet<String>();
		
		@Override
		public void notifyResponse(Response response) {
			Logger.log(VERBOSITY.DEBUG, tag, "Response notification "+response.toString());
			
			if (!response.getClass().equals(Notification.class)) tokenHandler.releaseToken(response.getToken());
			
			if (response.getClass().equals(SubscribeResponse.class)) {
				spuIds.add(((SubscribeResponse)response).getSPUID());
			}
		
			if(response.getClass().equals(UnsubscribeResponse.class)) {
				spuIds.remove(((UnsubscribeResponse)response).getSPUID());
			}
				
			synchronized(socket) {
				if (socket != null) 
					if (socket.isConnected()) socket.send(response.toString());	
			}
		}
		
		public Set<String> getSPUIDs() {
			return spuIds;
		}
		
		public SEPAResponseListener(WebSocket socket) {
			this.socket = socket;
		}
	}
	
	public WebSocketGate(Properties properties,TokenHandler tokenHandler,RequestResponseHandler requestHandler){
		this.tokenHandler = tokenHandler;
		if (tokenHandler == null) Logger.log(VERBOSITY.ERROR, tag, "Token handler is null");
		
		this.requestHandler = requestHandler;
		if (requestHandler == null) Logger.log(VERBOSITY.ERROR, tag, "Request handler is null");
		
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
		else {
			wsPort = Integer.parseInt(properties.getProperty("wsPort", "7777"));
			keepAlivePeriod =  Integer.parseInt(properties.getProperty("keepAlivePeriod", "5000"));
		}
	}
	
	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		Logger.log(VERBOSITY.DEBUG, tag, "onClose: "+socket.toString());
		
		if (keepAlivePeriod == 0) {
			for (String spuid: activeSockets.get(socket).getSPUIDs()){
				Integer token = tokenHandler.getToken();
				Logger.log(VERBOSITY.DEBUG, tag, "Add unsubscribe request "+token);
				requestHandler.addRequest(new UnsubscribeRequest(token,spuid),this);		
			}
			activeSockets.remove(socket); 
		}
	}

	@Override
	public void onConnect(WebSocket socket) {
		Logger.log(VERBOSITY.DEBUG, tag, "onConnect: "+socket.toString());
		SEPAResponseListener listener = new SEPAResponseListener(socket);
		activeSockets.put(socket, listener);
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		Logger.log(VERBOSITY.DEBUG, tag, "onMessage: "+socket.toString()+" message:"+text);
		Integer token = tokenHandler.getToken();
		Request request = parseRequest(token,text);
		if(request == null) {
			tokenHandler.releaseToken(token);
			//TODO SPARQL 1.1 Subscribe language
			socket.send("Not supported request: "+text);
			return;
		}
		
		requestHandler.addRequest(request,activeSockets.get(socket));	
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
			Logger.log(VERBOSITY.INFO, tag, "Failed to start WebSocket gate on port "+wsPort+ " "+e.getMessage());
			return false;
		}
		
		Logger.log(VERBOSITY.INFO, tag, "Started on port "+wsPort);

		if (keepAlivePeriod > 0) {
			new KeepAlive().start();
		}
		return true;
	}
	
	public class KeepAlive extends Thread implements ResponseListener{
		public void run() {
			while(true) {
				try {
					Thread.sleep(keepAlivePeriod);
				} catch (InterruptedException e) {
					return;
				}
				synchronized(activeSockets) {
					HashSet<WebSocket> disconnectedSockets = new HashSet<WebSocket>();
					for(WebSocket socket : activeSockets.keySet()) {	
						Date date = new Date();
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
						String timestamp = sdf.format(date);
						if (socket.isConnected()) socket.send("{\"ping\":\""+timestamp +"\"}");
						else disconnectedSockets.add(socket);
					}
					for (WebSocket socket : disconnectedSockets) {
						for (String spuid: activeSockets.get(socket).getSPUIDs()){
							Integer token = tokenHandler.getToken();
							Logger.log(VERBOSITY.DEBUG, tag, "Add unsubscribe request "+token);
							requestHandler.addRequest(new UnsubscribeRequest(token,spuid),this);		
						}
						
						activeSockets.remove(socket);
					}
				}
			}
		}

		@Override
		public void notifyResponse(Response response) {
			Logger.log(VERBOSITY.DEBUG, tag, "Response notification "+response.toString());
			tokenHandler.releaseToken(response.getToken());
			
		}
	}

	@Override
	public void notifyResponse(Response response) {
		Logger.log(VERBOSITY.DEBUG, tag, "Response notification "+response.toString());
		tokenHandler.releaseToken(response.getToken());
	}
}
