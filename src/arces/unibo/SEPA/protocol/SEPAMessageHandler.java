package arces.unibo.SEPA.protocol;

import javax.websocket.MessageHandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import arces.unibo.SEPA.commons.response.Notification;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

class SEPAMessageHandler implements MessageHandler.Whole<String> {
	private static final Logger logger = LogManager.getLogger("Message handler");
	
	private NotificationHandler handler;
	private SocketWatchdog watchDog;
	private SEPAEndpoint wsClient;
	
	public SEPAMessageHandler(NotificationHandler handler,SocketWatchdog watchDog,SEPAEndpoint wsClient) {
		this.handler = handler;
		this.watchDog = watchDog;
		this.wsClient = wsClient;
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
  	  		wsClient.close();
  	  		
  	  		watchDog.unsubscribed();
  	  		return;
  	  	}
  	  	
  	  	//Notification
  	  	if (notify.get("results") != null) {
  		  handler.semanticEvent(new Notification(notify));
  	  }	
	}
	
}