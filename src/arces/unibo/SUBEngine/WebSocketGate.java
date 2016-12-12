package arces.unibo.SUBEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;
import arces.unibo.SUBEngine.RequestResponseHandler.ResponseListener;

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
	
	private int PORT = 7979;
	
	private HashMap<Integer,WebSocket> pendingRequests = new HashMap<Integer,WebSocket>();
	private HashMap<String,WebSocket> activeSubscriptions = new HashMap<String,WebSocket>();
	
	public WebSocketGate(Properties properties,TokenHandler tokenHandler,RequestResponseHandler requestHandler){
		this.tokenHandler = tokenHandler;
		if (tokenHandler == null) Logger.log(VERBOSITY.ERROR, tag, "Token handler is null");
		
		this.requestHandler = requestHandler;
		if (requestHandler == null) Logger.log(VERBOSITY.ERROR, tag, "Request handler is null");
		
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
		else {
			if (properties.getProperty("wsPort")!= null) PORT = Integer.parseInt(properties.getProperty("wsPort", "7979"));
		}
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		Logger.log(VERBOSITY.DEBUG, tag, "onMessage: "+text);
		Integer token = tokenHandler.getToken();
		Request request = parseRequest(token,text);
		if(request == null) {
			tokenHandler.releaseToken(token);
			//TODO SPARQL 1.1 Subscribe language
			socket.send("Not supported request: "+text);
			return;
		}
		
		pendingRequests.put(token, socket);
		requestHandler.addRequest(request,this);	
	}
	
	//TODO SPARQL 1.1 Subscribe language
	private Request parseRequest(Integer token,String request) {
		String[] primitive = request.split("=");
		if (primitive == null) return null;
		if (primitive.length != 2) return null;
		if (primitive[0].startsWith("subscribe")) new SubscribeRequest(token,primitive[1]);
		if (primitive[0].startsWith("unsubscribe")) new SubscribeRequest(token,primitive[1]);
		return null;
	}
	
	@Override
	public void notifyResponse(Response response) {
		WebSocket socket = null;
		
		if (response.getClass().equals(SubscribeResponse.class)) {
			Logger.log(VERBOSITY.DEBUG, tag, "SUBSCRIBE "+response.getString());

			socket = pendingRequests.get(response.getToken());
			
			activeSubscriptions.put(((SubscribeResponse) response).getSPUID(),socket);		
			
			tokenHandler.releaseToken(response.getToken());
		}
		if (response.getClass().equals(UnsubscribeResponse.class)) {
			Logger.log(VERBOSITY.DEBUG, tag, "UNSUBSCRIBE "+response.getString());

			socket = pendingRequests.get(response.getToken());
			
			activeSubscriptions.remove(((UnsubscribeResponse) response).getSPUID());			
			
			tokenHandler.releaseToken(response.getToken());
		}
		if (response.getClass().equals(Notification.class)) {
			Logger.log(VERBOSITY.DEBUG, tag, "NOTIFICATION "+response.getString());
			
			socket = activeSubscriptions.get(((Notification) response).getSPUID());		
		}
		
		//TODO SPARQL 1.1 Subscribe language
		if (socket != null) socket.send(response.getString());
	}
	
	public int getPort() {
		return PORT;
	}
	
	public boolean start(){
		final HttpServer server = HttpServer.createSimpleServer("/var/www", PORT);

        // Register the WebSockets add on with the HttpServer
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());

        // register the application
        WebSocketEngine.getEngine().register("", "/sparql", this);

        try {
			server.start();
		} catch (IOException e) {
			Logger.log(VERBOSITY.INFO, tag, "Failed to start WebSocket gate on port "+PORT+ " "+e.getMessage());
			return false;
		}
		
		Logger.log(VERBOSITY.INFO, tag, "Started on port "+PORT);

		return true;
	}
}
