package arces.unibo.SEPA.client.api;

import javax.websocket.MessageHandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.NotificationHandler;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

class WebsocketMessageHandler implements MessageHandler.Whole<String> {
	private static final Logger logger = LogManager.getLogger("WebsocketMessageHandler");
	
	private NotificationHandler handler;
	private WebsocketWatchdog watchDog;
	private WebsocketClientEndpoint wsClient;
	
	public WebsocketMessageHandler(NotificationHandler handler,WebsocketWatchdog watchDog,WebsocketClientEndpoint wsClient) {
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
  	  		SubscribeResponse response;
	  		if (notify.get("alias") != null) 
	  			response = new SubscribeResponse(notify.get("subscribed").getAsString(),notify.get("alias").getAsString());
	  		else
	  		response = new SubscribeResponse(notify.get("subscribed").getAsString());
	  		handler.subscribeConfirmed(response);
  	  		
  	  		if (!watchDog.isAlive()) watchDog.start();
  	  		watchDog.subscribed();
  	  		return;
  	  	}
  	  	
  	  	//Unsubscribe confirmed
  	  	if (notify.get("unsubscribed") != null) {
  	  		handler.unsubscribeConfirmed(new UnsubscribeResponse(notify.get("unsubscribed").getAsString()));
  	  		
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