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

package arces.unibo.SEPA.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glassfish.tyrus.client.ClientManager;

import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.response.Notification;

public class SecureEventProtocol {
	
	public interface NotificationHandler {
		public void semanticEvent(Notification notify);
		public void subscribeConfirmed(String spuid);
		public void unsubscribeConfirmed(String spuid);
		public void ping();
		public void rawData();
	}
	
	private String tag ="SEProtocol";
	private final String propertiesFile = "client.properties";
	private Properties properties = new Properties();
		
	private String postUrl;
	private String wsUrl;
	
	private NotificationHandler handler = null;
	private SEPAEndpoint wsClient;
	
	public String getUpdateURL() {
		return postUrl;
	}
	
	public String getSubscribeURL() {
		return wsUrl;
	}
	
	public SecureEventProtocol(String url, int updatePort,int subscribePort, String path) {				
		postUrl = "http://"+url+":"+updatePort+path;
		wsUrl = "ws://"+url+":"+subscribePort+path;
		
		properties.setProperty("updatePort", Integer.toString(updatePort));
		properties.setProperty("subscribePort", Integer.toString(subscribePort));
		properties.setProperty("path", path);
		properties.setProperty("url", url);
		
		storeProperties(propertiesFile);
		
		wsClient = new SEPAEndpoint(wsUrl);
	}

	public SecureEventProtocol() {
		loadProperties(propertiesFile);
		
		int updatePort = Integer.parseInt(properties.getProperty("updatePort", Integer.toString(8000)));
		int subscribePort = Integer.parseInt(properties.getProperty("subscribePort", Integer.toString(9000)));
		String path = properties.getProperty("path", "/sparql");
		String url = properties.getProperty("url", "localhost");
		
		postUrl = "http://"+url+":"+updatePort+path;
		wsUrl = "ws://"+url+":"+subscribePort+path;
		
		properties.setProperty("updatePort", Integer.toString(updatePort));
		properties.setProperty("subscribePort", Integer.toString(subscribePort));
		properties.setProperty("path", path);
		properties.setProperty("url", url);
		
		storeProperties(propertiesFile);
		
		wsClient = new SEPAEndpoint(wsUrl);
	}

	public boolean update(String sparql) {
		return new JsonParser().parse(SPARQLPrimitive(sparql,true)).getAsJsonObject().get("status").getAsBoolean();
	}
	
	public BindingsResults query(String sparql) {
		JsonObject json = new JsonParser().parse(SPARQLPrimitive(sparql,false)).getAsJsonObject();
		
		if(!json.get("status").getAsBoolean()) return null;
		
		return new BindingsResults(new JsonParser().parse(json.get("body").getAsString()).getAsJsonObject());
	}
	
	class SEPAEndpoint extends Endpoint {
		private Session wsClientSession = null;;
		private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		private ClientManager client = ClientManager.createClient();
		private SEPAMessageHandler messageHandler = new SEPAMessageHandler();
		private String wsUrl;
		private String sparql;
		
		@Override
		public void onOpen(Session session, EndpointConfig arg1) {
			wsClientSession = session;
        	wsClientSession.addMessageHandler(messageHandler);	
        	try {
				wsClientSession.getBasicRemote().sendText("subscribe="+sparql);
			} 
			catch (IOException e) {}
		}
		
		public SEPAEndpoint(String wsUrl) {
			this.wsUrl = wsUrl;
		}
		
		private boolean connect() {
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
		
		private boolean isConnected() {
			if (wsClientSession == null) return false;
			return wsClientSession.isOpen();
		}
		
		public boolean subscribe(String sparql) {
			if (isConnected())
				try {
					wsClientSession.getBasicRemote().sendText("subscribe="+sparql);
				} 
				catch (IOException e) {
					return false;
				}
			else {
				this.sparql = sparql;
				return connect();	
			}
		
			return true;
		}
		
		public boolean unsubscribe(String spuid) {
			if (isConnected())
				try {
					wsClientSession.getBasicRemote().sendText("unsubscribe="+spuid);
				} 
				catch (IOException e) {
					return false;
				}
			
			return true;
		}
		
		public boolean close() {
			try {
				wsClientSession.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}
	
	class SEPAMessageHandler implements MessageHandler.Whole<String> {

		@Override
		public void onMessage(String message) {
			Logger.log(VERBOSITY.DEBUG, tag, message);
			if (handler == null) {
				Logger.log(VERBOSITY.WARNING, tag, "Notification handler is NULL");
				return;
			}
      	  	
			JsonObject notify = new JsonParser().parse(message).getAsJsonObject();
				
      	  	//Ping
      	  	if(notify.get("ping") != null) {
      	  		handler.ping();
      	  		return;
      	  	}
			 
      	  	//Subscribe confirmed
      	  	if (notify.get("subscribed") != null) {
      	  		handler.subscribeConfirmed(notify.get("subscribed").getAsString());
      	  		return;
      	  	}
      	  	
      	  	//Unsubscribe confirmed
      	  	if (notify.get("unsubscribed") != null) {
      	  		handler.unsubscribeConfirmed(notify.get("unsubscribed").getAsString());
      	  		wsClient.close();
      	  		return;
      	  	}
      	  	
      	  	//Notification
      	  	if (notify.get("notification") != null) {
      		  handler.semanticEvent(new Notification(notify));
      	  }	
		}
		
	}
	
	public boolean subscribe(String sparql,NotificationHandler handler) {
		this.handler = handler;
		return wsClient.subscribe(sparql);
	}

	public boolean unsubscribe(String subID) {
		return wsClient.unsubscribe(subID);
	}
	
	private String SPARQLPrimitive(String sparql,boolean update){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		//HTTP POST for query and update
		//POST Headers
		HttpPost postRequest = new HttpPost(postUrl);
		postRequest.setHeader("Accept", "application/sparql-results+json");	
		if(update) postRequest.setHeader("Content-Type", "application/sparql-update");
		else postRequest.setHeader("Content-Type", "application/sparql-query");
		
		//POST body
		HttpEntity body;
		try {
			body = new ByteArrayEntity(sparql.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Logger.log(VERBOSITY.ERROR, tag, e.getMessage());
			
			JsonObject json = new JsonObject();
			json.add("status", new JsonPrimitive(false));
        	json.add("body", new JsonPrimitive(e.getMessage()));
        	return json.toString();
		}
		if (body != null) postRequest.setEntity(body);
		
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
	        
	        @Override
	        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
	            int status = response.getStatusLine().getStatusCode();
	            JsonObject json = new JsonObject();
	            if (status >= 200 && status < 300) 
	            {
	                HttpEntity entity = response.getEntity();
	                if (entity != null) {
	                	json.add("status", new JsonPrimitive(true));
	                	json.add("body", new JsonPrimitive(EntityUtils.toString(entity)));
	                }
	                else {
	                	json.add("status", new JsonPrimitive(false));
	                	json.add("body", new JsonPrimitive("Http response entity is null. Response status: "+status));
	                }
	            } 
	            else 
	            {
	            	Logger.log(VERBOSITY.ERROR, tag, "Unexpected response status: " + status);
	            	json.add("status", new JsonPrimitive(false));
                	json.add("body", new JsonPrimitive("Http response entity is null. Response status: "+status));
	            }
	            return json.toString();
	        }
		};
		
		String responseBody;
		try {
			long timing = System.nanoTime();
	    	
			responseBody = httpclient.execute(postRequest, responseHandler);
	    	
			timing = System.nanoTime() - timing;
	    	
			if(update) Logger.log(VERBOSITY.INFO, "timing", "UpdateTime "+timing+ " ns");
	    	else Logger.log(VERBOSITY.INFO, "timing", "QueryTime "+timing+ " ns");
	    }
	    catch(IOException e) {
	    	Logger.log(VERBOSITY.ERROR, tag, e.getMessage());
	    	
	    	JsonObject json = new JsonObject();
			json.add("status", new JsonPrimitive(false));
        	json.add("body", new JsonPrimitive(e.getMessage()));
        	return json.toString();	      	
	    } 

		return responseBody;
	}
	
	private boolean loadProperties(String fname){
		FileInputStream in;
		try {
			in = new FileInputStream(fname);
		} catch (FileNotFoundException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+fname);
			return false;
		}
		try {
			properties.load(in);
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on loading properties file: "+fname);
			return false;
		}
		try {
			in.close();
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+fname);
			return false;
		}
		
		return true;	
	}

	private boolean storeProperties(String fname) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(fname);
		} catch (FileNotFoundException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+fname);
			return false;
		}
		try {
			properties.store(out, "---SEPA client properties file ---");
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on storing properties file: "+fname);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+fname);
			return false;
		}
		
		return true;

	}
}
