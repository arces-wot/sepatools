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
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.Notification;
import arces.unibo.SEPA.commons.BindingsResults;

public class SecureEventProtocol {
	
	public interface NotificationHandler {
		public void notify(Notification notify);
	}
	
	private String tag ="SEProtocol";
	private final String propertiesFile = "client.properties";
	private Properties properties = new Properties();
		
	private String postUrl;
	private String wsUrl;
	
	private String syncResponse = null;
	private NotificationHandler handler = null;
	private boolean syncRequest = false;
	private Session wsClientSession = null;
	
	public SecureEventProtocol(String url, int updatePort,int subscribePort, String path) {				
		postUrl = "http://"+url+":"+updatePort+path;
		wsUrl = "ws://"+url+":"+subscribePort+path;
		
		properties.setProperty("updatePort", Integer.toString(updatePort));
		properties.setProperty("subscribePort", Integer.toString(subscribePort));
		properties.setProperty("path", path);
		properties.setProperty("url", url);
		
		storeProperties(propertiesFile);
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
	}

	public boolean update(String sparql) {
		return new JsonParser().parse(SPARQLPrimitive(sparql,true)).getAsJsonObject().get("status").getAsBoolean();
	}
	
	public BindingsResults query(String sparql) {
		JsonObject json = new JsonParser().parse(SPARQLPrimitive(sparql,false)).getAsJsonObject();
		
		if(!json.get("status").getAsBoolean()) return null;
		
		return new BindingsResults(new JsonParser().parse(json.get("body").getAsString()).getAsJsonObject());
	}

	private String sendSync(String string) {
		syncRequest  = true;
		syncResponse = "timeout";
		
		if(wsClientSession.isOpen()) {
			try {
				wsClientSession.getBasicRemote().sendText(string);
			} catch (IOException e) 
			{
				JsonObject res = new JsonObject();
				
				if (syncRequest == true) res.add("status", new JsonPrimitive(false));
				else res.add("status", new JsonPrimitive(true));
				
				res.add("body", new JsonPrimitive(e.getMessage()));
				
				return res.toString();
			}
			waitResponse();
		}
		
		JsonObject res = new JsonObject();
		res.add("status", new JsonPrimitive(!syncRequest));
		res.add("body", new JsonPrimitive(syncResponse));
		
		return res.toString();
	}
	
	public synchronized void waitResponse() {
	    while(syncRequest) {
	        try {
	            wait(2000);
	        } catch (InterruptedException e) {}
	    }
	}
	
	public synchronized void notifyResponse(String message) {
		syncResponse = message;
		syncRequest = false;
	    notifyAll();
	}
	
	public String subscribe(String sparql, NotificationHandler mHandler) {
		try {
			this.handler = mHandler;
			
			final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
			
			ClientManager client = ClientManager.createClient();
	        
			client.connectToServer(new Endpoint() {
	            @Override
	            public void onOpen(Session session, EndpointConfig config) {
	            	wsClientSession = session;
	            	wsClientSession.addMessageHandler(new MessageHandler.Whole<String>() {
                       
	            		@Override
                        public void onMessage(String message) {
                        	Logger.log(VERBOSITY.DEBUG, tag, message);
                        	
  			        	  	JsonObject notify = new JsonParser().parse(message).getAsJsonObject();
  			  				if(notify.get("ping") != null) return;
  			  			 
  			  				if (syncRequest) {
  			  					notifyResponse(message);
  			  				}
  			  				else {
  			  					String spuid = notify.get("spuid").getAsString();
  			  					
  			  					Integer sequence = notify.get("sequence").getAsInt();
  			  					
  			  					ARBindingsResults results = new ARBindingsResults(notify);
  			  					
  			  					Notification n = new Notification(spuid,results,sequence);
  			  					
  			  					if(handler != null) handler.notify(n);
  			  				}	
                        }
                    });
	            }
	        }, cec, new URI(wsUrl));
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		String response = sendSync("subscribe="+sparql);
		
		JsonObject json = new JsonParser().parse(response).getAsJsonObject();
		
		if(json.get("status").getAsBoolean()) return new JsonParser().parse(json.get("body").getAsString()).getAsJsonObject().get("spuid").getAsString();
		else return null;
	}

	public boolean unsubscribe(String subID) {
		if (wsClientSession == null) return false;
		if (!wsClientSession.isOpen()) return false;
		
		String response = sendSync("unsubscribe="+subID);
		
		JsonObject json = new JsonParser().parse(response).getAsJsonObject();
		
		if(json.get("status").getAsBoolean()) {
			try {
				wsClientSession.close();
				wsClientSession = null;
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	private String SPARQLPrimitive(String sparql,boolean update){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost postRequest = new HttpPost(postUrl);
		postRequest.setHeader("Accept", "application/sparql-results+json");
		
		if(update) postRequest.setHeader("Content-Type", "application/sparql-update");
		else postRequest.setHeader("Content-Type", "application/sparql-query");
		
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
