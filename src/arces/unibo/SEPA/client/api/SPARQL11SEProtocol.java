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

package arces.unibo.SEPA.protocol;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.application.SEPALogger;
import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;

public class SecureEventProtocol {
	
	private String tag ="SEProtocol";
	private final String propertiesFile = "client.properties";
	private Properties properties = new Properties();
		
	private String postUrl;
	private String wsUrl;
	private SEPAEndpoint wsClient;
	
	private static final Logger logger = LogManager.getLogger("SE Protocol");
	
	public enum SUBSCRIPTION_STATE {SUBSCRIBED,UNSUBSCRIBED,BROKEN_SOCKET};
	
	public String getUpdateURL() {
		return postUrl;
	}
	
	public String getSubscribeURL() {
		return wsUrl;
	}
	
	private void init(String url, int updatePort,int subscribePort, String path) {
		postUrl = "http://"+url+":"+updatePort+path;
		wsUrl = "ws://"+url+":"+subscribePort+path;
		
		properties.setProperty("updatePort", Integer.toString(updatePort));
		properties.setProperty("subscribePort", Integer.toString(subscribePort));
		properties.setProperty("path", path);
		properties.setProperty("url", url);
		
		wsClient = new SEPAEndpoint(wsUrl);
		
		storeProperties(propertiesFile);
	}
	
	public SecureEventProtocol(String url, int updatePort,int subscribePort, String path) {				
		init(url,updatePort,subscribePort,path);
	}

	public SecureEventProtocol() {
		loadProperties(propertiesFile);
		
		int updatePort = Integer.parseInt(properties.getProperty("updatePort", Integer.toString(8000)));
		int subscribePort = Integer.parseInt(properties.getProperty("subscribePort", Integer.toString(9000)));
		String path = properties.getProperty("path", "/sparql");
		String url = properties.getProperty("url", "localhost");
		
		init(url,updatePort,subscribePort,path);
	}

	public boolean update(String sparql) {
		JsonObject response = new JsonParser().parse(SPARQLPrimitive(sparql,true)).getAsJsonObject();
		int code = response.get("code").getAsInt();
		if (code >= 200 && code <= 300) return true;
		logger.error(response.get("body"));
		return false;
	}
	
	public BindingsResults query(String sparql) {
		JsonObject response = new JsonParser().parse(SPARQLPrimitive(sparql,false)).getAsJsonObject();
		int code = response.get("code").getAsInt();
		if (code >= 200 && code <= 300) return new BindingsResults(new JsonParser().parse(response.get("body").getAsString()).getAsJsonObject());
		
		return null;
	}
	
	public boolean subscribe(String sparql,NotificationHandler handler) {
		return wsClient.subscribe(sparql,handler);
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
			SEPALogger.log(VERBOSITY.ERROR, tag, e.getMessage());
			
			JsonObject json = new JsonObject();
			json.add("code", new JsonPrimitive(500));
        	json.add("body", new JsonPrimitive(e.getMessage()));
        	return json.toString();
		}
		if (body != null) postRequest.setEntity(body);
		
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
	        
	        @Override
	        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
	            
	        	int code = response.getStatusLine().getStatusCode();
	        	String body = EntityUtils.toString(response.getEntity());
	        	
	            JsonObject json = new JsonObject();
	            json.add("code", new JsonPrimitive(code));
            	json.add("body", new JsonPrimitive(body));
	            
	            return json.toString();
	        }
		};
		
		String responseBody;
		try {
			long timing = System.nanoTime();
	    	
			responseBody = httpclient.execute(postRequest, responseHandler);
	    	
			timing = System.nanoTime() - timing;
	    	
			if(update) SEPALogger.log(VERBOSITY.INFO, "timing", "UpdateTime "+timing+ " ns");
	    	else SEPALogger.log(VERBOSITY.INFO, "timing", "QueryTime "+timing+ " ns");
	    }
	    catch(IOException e) {
	    	SEPALogger.log(VERBOSITY.ERROR, tag, e.getMessage());
	    	
	    	JsonObject json = new JsonObject();
			json.add("code", new JsonPrimitive(500));
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
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+fname);
			return false;
		}
		try {
			properties.load(in);
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on loading properties file: "+fname);
			return false;
		}
		try {
			in.close();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+fname);
			return false;
		}
		
		return true;	
	}

	private boolean storeProperties(String fname) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(fname);
		} catch (FileNotFoundException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+fname);
			return false;
		}
		try {
			properties.store(out, "---SEPA client properties file ---");
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on storing properties file: "+fname);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+fname);
			return false;
		}
		
		return true;

	}
}
