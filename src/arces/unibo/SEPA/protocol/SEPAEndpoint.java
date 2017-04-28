package arces.unibo.SEPA.protocol;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.commons.response.Notification;

class SEPAEndpoint extends Endpoint implements MessageHandler.Whole<String> {
	private Session wsClientSession = null;;
	private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
	private ClientManager client = ClientManager.createClient();
	
	private String sparql;
	private String wsUrl;
	
	private static final Logger logger = LogManager.getLogger("Message handler");
	
	private NotificationHandler handler;
	private SocketWatchdog watchDog = null;
	
	public SEPAEndpoint(String wsUrl) {
		this.wsUrl = wsUrl;
	}
	
	@Override
	public void onOpen(Session session, EndpointConfig arg1) {
		logger.debug("onOpen");
		wsClientSession = session;
    	wsClientSession.addMessageHandler(this);	
    	try {
    		JsonObject request = new JsonObject();
			request.add("subscribe", new JsonPrimitive(sparql));
			wsClientSession.getBasicRemote().sendText(request.toString());
		} 
		catch (IOException e) {}
	}
	
	private boolean connect() {
		logger.debug("connect");
		try {
			client.connectToServer(this,cec, new URI(wsUrl));
		} catch (DeploymentException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	boolean isConnected() {
		if (wsClientSession == null) return false;
		return wsClientSession.isOpen();
	}
	
	public boolean subscribe(String sparql,NotificationHandler handler) {
		logger.debug("subscribe");
		this.handler = handler;
		this.sparql = sparql;
		if (isConnected())
			try {
				JsonObject request = new JsonObject();
				request.add("subscribe", new JsonPrimitive(sparql));
				wsClientSession.getBasicRemote().sendText(request.toString());
			} 
			catch (IOException e) {
				return false;
			}
		else {
			return connect();	
		}
		
		//Start watchdog
		if (watchDog == null) watchDog = new SocketWatchdog(handler,this,sparql); 
	
		return true;
	}
	
	public boolean unsubscribe(String spuid) {
		logger.debug("unsubscribe");
		if (isConnected())
			try {
				JsonObject request = new JsonObject();
				request.add("unsubscribe", new JsonPrimitive(spuid));
				wsClientSession.getBasicRemote().sendText(request.toString());
			} 
			catch (IOException e) {
				return false;
			}
		
		return true;
	}
	
	public boolean close() {
		logger.debug("close");
		try {
			wsClientSession.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void onMessage(String message) {
		logger.debug(message);
		if (handler == null) {
			logger.warn("Notification handler is NULL");
			return;
		}
  	  	
		JsonObject notify = new JsonParser().parse(message).getAsJsonObject();
			
  	  	//Ping
  	  	if(notify.get("ping") != null) {
  	  		handler.ping();
  	  		
  	  		watchDog.ping();
  	  		return;
  	  	}
		 
  	  	//Subscribe confirmed
  	  	if (notify.get("subscribed") != null) {
  	  		handler.subscribeConfirmed(notify.get("subscribed").getAsString());
  	  		
  	  		if (!watchDog.isAlive()) watchDog.start();
  	  		watchDog.subscribed();
  	  		return;
  	  	}
  	  	
  	  	//Unsubscribe confirmed
  	  	if (notify.get("unsubscribed") != null) {
  	  		handler.unsubscribeConfirmed(notify.get("unsubscribed").getAsString());
  	  		
  	  		//wsClient.close();
  	  		try {
				wsClientSession.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
  	  	
  	  		watchDog.unsubscribed();
  	  		return;
  	  	}
  	  	
  	  	//Notification
  	  	if (notify.get("results") != null) {
  		  handler.semanticEvent(new Notification(notify));
  	  }
		
	}
}
