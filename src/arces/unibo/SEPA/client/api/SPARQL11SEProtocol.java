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

package arces.unibo.SEPA.client.api;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import arces.unibo.SEPA.client.api.SPARQL11Properties.QueryResultsFormat;
import arces.unibo.SEPA.client.api.SPARQL11Properties.SPARQLPrimitive;
import arces.unibo.SEPA.client.api.SPARQL11SEProperties.SPARQL11SEPrimitive;

import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.request.RegistrationRequest;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.JWTResponse;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.NotificationHandler;
import arces.unibo.SEPA.commons.response.QueryResponse;
import arces.unibo.SEPA.commons.response.RegistrationResponse;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UpdateResponse;

public class SPARQL11SEProtocol extends SPARQL11Protocol {
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProtocol");
	
	private WebsocketClientEndpoint wsClient;
	
	public enum SUBSCRIPTION_STATE {SUBSCRIBED,UNSUBSCRIBED,BROKEN_SOCKET};
	
	protected SPARQL11SEProperties properties = null;
	
	protected class SEPAHostnameVerifier implements HostnameVerifier {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			// TODO IMPORTANT Verify X.509 certificate
			
			return true;
		}
		
	}
	
	private SSLConnectionSocketFactory getSSLConnectionSocketFactory() {
		// Trust own CA and all self-signed certificates
        SSLContext sslcontext = null;
		try {
			sslcontext = SSLContexts.custom()
			        .loadTrustMaterial(new File("sepa.jks"), "*sepa.jks*".toCharArray(),new TrustSelfSignedStrategy())
			        .build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException
				| IOException e1) {
			logger.error(e1.getMessage());
			return null;
		}
		
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext,
                new String[] { "TLSv1" },
                null,
                new SEPAHostnameVerifier());
        return 	sslsf;
	}
	
	public SPARQL11SEProtocol(SPARQL11SEProperties properties) {
		super(properties);
		if (properties == null) {
			logger.fatal("Properties are null");
			return;
		}
				
		this.properties = properties;
		
		//Create secure HTTP client
		SSLConnectionSocketFactory sslSocketFactory = getSSLConnectionSocketFactory();		
		if (sslSocketFactory != null) httpclient = HttpClients.custom().setSSLSocketFactory(sslSocketFactory).build();
        
		//Create secure WebSocket client
		if (properties.getWsScheme().equals("ws"))
			wsClient = new WebsocketClientEndpoint(properties.getWsScheme()+"://"+properties.getHost()+":"+properties.getWsPort()+properties.getSubscribePath());
		else
			wsClient = new SecureWebsocketClientEndpoint(properties.getWsScheme()+"://"+properties.getHost()+":"+properties.getWsPort()+properties.getSubscribePath());
		
		//HTTP response handler
		responseHandler = new ResponseHandler<String>() {
	        @Override
	        public String handleResponse(final HttpResponse response) {	            
	        	String body = null;
	        	
	        	HttpEntity entity = response.getEntity();
	        	
	            try {
					body = EntityUtils.toString(entity,Charset.forName("UTF-8"));
				} catch (ParseException e) {
					body = e.getMessage();
				} catch (IOException e) {
					body = e.getMessage();
				}
	            return body;
	        }
      };
	}

	//SPARQL 1.1 Update Primitive
	public Response update(UpdateRequest request) {
		return super.update(request);
	}
	
	//SPARQL 1.1 Query Primitive
	public Response query(QueryRequest request) {
		return super.query(request);
	}
	
	//SPARQL 1.1 SE Subscribe Primitive
	public Response subscribe(SubscribeRequest sub,NotificationHandler handler) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SUBSCRIBE, sub,handler);		
	}
	
	//SPARQL 1.1 SE Unsubscribe Primitive
	public Response unsubscribe(UnsubscribeRequest subID) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.UNSUBSCRIBE, subID);
	}
	
	//SPARQL 1.1 SE SECURE Subscribe Primitive
	public Response secureSubscribe(SubscribeRequest sub, NotificationHandler handler) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECURESUBSCRIBE, sub,handler);
	}	
	
	//SPARQL 1.1 SE SECURE Unsubscribe Primitive
	public Response secureUnsubscribe(UnsubscribeRequest subID) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREUNSUBSCRIBE, subID);	
	}
	
	//SPARQL 1.1 SE SECURE Update Primitive
	public Response secureUpdate(UpdateRequest request) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREUPDATE, request);
	}
	
	//SPARQL 1.1 SE SECURE Query Primitive
	public Response secureQuery(QueryRequest query) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREQUERY, query);
	}
	
	//Registration to the Authorization Server (AS)
	public Response register(String identity) {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.REGISTER, identity);
	}
	
	//Token request to the Authorization Server (AS)
	public Response requestToken() {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.REQUESTTOKEN);
	}
	
	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op,Object request) {
		return executeSPARQL11SEPrimitive(op,request,null);
	}
	
	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op) {
		return executeSPARQL11SEPrimitive(op,null,null);
	}
	
	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op,Object request,NotificationHandler handler) {
		//Properties MUST not be null
		if (properties == null) {
			logger.fatal("Properties are null");
			return new ErrorResponse(0,500,"Properties are null");
		}		
		
		//Not secure subscriptions
		if (op.equals(SPARQL11SEPrimitive.SUBSCRIBE) || op.equals(SPARQL11SEPrimitive.UNSUBSCRIBE)) {
			if (!properties.getWsScheme().equals("ws")) {
				logger.fatal("WS scheme required");
				return new ErrorResponse(0,405,"WS scheme required");	
			}
			
			if (op.equals(SPARQL11SEPrimitive.SUBSCRIBE)) {
				logger.debug("SUBSCRIBE");
				if(wsClient.subscribe(((SubscribeRequest) request).getSPARQL(),((SubscribeRequest) request).getAlias(),null,handler)){
					return new SubscribeResponse(0,null);
				}
					else return new ErrorResponse(0,500);
			}
			else
			{	
				logger.debug("UNSUBSCRIBE");
				if(wsClient.unsubscribe(((UnsubscribeRequest) request).getSubscribeUUID(),null)) {
					return new SubscribeResponse(0,null);
				 }
				 else return new ErrorResponse(0,500);
			}
		}
				
		//Check authorization
		if (op.equals(SPARQL11SEPrimitive.REQUESTTOKEN)) {
			logger.debug("REQUEST TOKEN");
			String basic = properties.getBasicAuthorization();
			if (basic == null) return new ErrorResponse(0,401,"Basic authorization in null. Register first");
		} else if (!op.equals(SPARQL11SEPrimitive.REGISTER)) {
			logger.debug("QUERY, UPDATE or SUBSCRIBE (secure)");
			String token = properties.getAccessToken();
			if (token == null) return new ErrorResponse(0,401,"Bearer authorization in null. Request token first");
		}

		//Secure subscriptions
		if (op.equals(SPARQL11SEPrimitive.SECURESUBSCRIBE) || op.equals(SPARQL11SEPrimitive.SECUREUNSUBSCRIBE)) {
			if (!properties.getWsScheme().equals("wss")) {
				logger.fatal("WSS scheme is required");
				return new ErrorResponse(0,405,"WSS scheme is required");	
			}
			
			if (op.equals(SPARQL11SEPrimitive.SECURESUBSCRIBE)) {
				logger.debug("SECURE SUBSCRIBE");
				if(wsClient.subscribe(((SubscribeRequest) request).getSPARQL(),((SubscribeRequest) request).getAlias(),properties.getAccessToken(),handler)) {
					return new SubscribeResponse(0,null);
				 }
				 else return new ErrorResponse(0,500);
			}
			else
			{
				logger.debug("SECURE UNSUBSCRIBE");
				if(wsClient.unsubscribe(((UnsubscribeRequest) request).getSubscribeUUID(),properties.getAccessToken())) {
					return new SubscribeResponse(0,null);
				 }
				 else return new ErrorResponse(0,500);
			}
		}
		
		//Secure HTTPS operations
		if (!properties.getHttpScheme().equals("https")) {
			logger.fatal("HTTPS scheme is required");
			return new ErrorResponse(0,405,"HTTPS scheme is required");	
		}
		
		//Create the HTTPS request
		URI uri;
		String path = null;
		if (op.equals(SPARQL11SEPrimitive.SECUREUPDATE)) {
			logger.debug("SECURE UPDATE");
			path = properties.getUpdatePath();
		}
		else if (op.equals(SPARQL11SEPrimitive.SECUREQUERY)) {
			logger.debug("SECURE QUERY");
			path = properties.getQueryPath();
		}
		else if (op.equals(SPARQL11SEPrimitive.REGISTER)) path = properties.getRegistrationPath();
		else if (op.equals(SPARQL11SEPrimitive.REQUESTTOKEN)) path = properties.getRequestTokenPath();
		else path = null;
		
		try {
			uri = new URI(properties.getHttpScheme(),
					   null,
					   properties.getHost(),
					   properties.getHttpPort(),
					   path,
					   "",
					   null);
		} catch (URISyntaxException e) {
			logger.fatal(e.getMessage());
			return new ErrorResponse(0,500,e.getMessage());
		}
		
		//Uses the HTTP POST method for all the operations
		HttpUriRequest httpRequest = new HttpPost(uri);
		
		//Headers and body
		String contentType = null;
		ByteArrayEntity body = null;
		String accept = null;
		String authorization = null;
		
		if (op.equals(SPARQL11SEPrimitive.REQUESTTOKEN)) {
			/*
			String token = properties.getAccessToken();
			if (token != null)  
				if (!properties.isTokenExpired()) {
					String jwt = properties.getAccessToken();
					String type = properties.getTokenType();
					long expires_in = properties.getExpiringSeconds();
					return new JWTResponse(jwt,type,expires_in);
				}
			*/
			authorization = "Basic "+properties.getBasicAuthorization();
			contentType = "application/x-www-form-urlencoded"; 
			accept ="application/json";
			logger.debug("Request access token: "+authorization);
		}	
		else if (op.equals(SPARQL11SEPrimitive.REGISTER)) {
			accept = "application/json";			
			contentType = "application/json";
			String identity = (String) request;
			
			try {
				body = new ByteArrayEntity(new RegistrationRequest(identity).toString().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				return new ErrorResponse(0,500,e.getMessage());
			}
		}
		else if (op.equals(SPARQL11SEPrimitive.SECUREUPDATE)) {		
			accept = "text/plain";			
			contentType = "application/x-www-form-urlencoded";
			authorization = "Bearer "+ properties.getAccessToken();	
			
			try {
				String encodedContent = URLEncoder.encode(((UpdateRequest)request).getSPARQL(), "UTF-8");
				body = new ByteArrayEntity(("update="+encodedContent).getBytes());
				body.setContentType(contentType);
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				return new ErrorResponse(0,500,e.getMessage());
			}
		}
		else if (op.equals(SPARQL11SEPrimitive.SECUREQUERY)) {
			accept = "application/sparql-results+json";			
			contentType = "application/sparql-query";	
			authorization = "Bearer "+ properties.getAccessToken();		
			
			try {
				body = new ByteArrayEntity(((QueryRequest)request).getSPARQL().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				return new ErrorResponse(0,500,e.getMessage());
			}
		}
		
		if (contentType != null) httpRequest.setHeader("Content-Type", contentType);
		if (accept != null) httpRequest.setHeader("Accept", accept);
		if (authorization != null) httpRequest.setHeader("Authorization", authorization);
		if (body != null) ((HttpPost) httpRequest).setEntity(body);
		
		//HTTP request execution
		String response = null;
		try {
			long timing = System.nanoTime();

			response = httpclient.execute(httpRequest, responseHandler);
			    	
			timing = System.nanoTime() - timing;
			
			if(op.equals(SPARQL11SEPrimitive.REGISTER)) logger.info("REGISTER "+timing/1000000+ " ms");
			else if(op.equals(SPARQL11SEPrimitive.REQUESTTOKEN)) logger.info("TOKEN "+timing/1000000+ " ms");
			else if(op.equals(SPARQL11SEPrimitive.SECUREQUERY)) logger.info("SECURE_QUERY "+timing/1000000+ " ms");
			else if(op.equals(SPARQL11SEPrimitive.SECUREUPDATE)) logger.info("SECURE_UPDATE "+timing/1000000+ " ms");
			
			logger.debug(response);
		}
		catch(java.net.ConnectException e) {
			logger.error(httpRequest.toString());
			logger.error(e.getMessage());
			return new ErrorResponse(0,503,e.getMessage());
		} 
		catch (ClientProtocolException e) {
			logger.error(httpRequest.toString());
			logger.error(e.getMessage());	
			return new ErrorResponse(0,500,e.getMessage());
		} 
		catch (IOException e) {
			logger.error(httpRequest.toString());
			logger.error(e.getMessage());
			return new ErrorResponse(0,500,e.getMessage());
		}
				
		//Parsing the response
		return parseSPARQL11SEResponse(response,op);
	}
	
	/**
	 * Parse SPARQL 1.1 Query and Update
	 * 
	 * Update and error responses are serialized as JSON objects:
	 * 
	 * {"code": HTTP Return Code, "body": "Message body"}
	 */
	
	@Override
	protected Response parseEndpointResponse(int token,String jsonResponse,SPARQLPrimitive op,QueryResultsFormat format) {
		logger.debug("Parse endpoint response #"+token+" "+jsonResponse);
		
		JsonObject json = null;
		try {
			json = new JsonParser().parse(jsonResponse).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			logger.warn(e.getMessage());
			
			if (op.equals(SPARQLPrimitive.UPDATE)) return new UpdateResponse(token,jsonResponse);
			
			return new ErrorResponse(token,500,e.getMessage());	
		}
		
		if (json.get("code") != null) {
			if (json.get("code").getAsInt() >= 400) return new ErrorResponse(token,json.get("code").getAsInt(),json.get("body").getAsString());	
		}
		
		if (op.equals(SPARQLPrimitive.UPDATE)) return new UpdateResponse(token,json.get("body").getAsString());
		if (op.equals(SPARQLPrimitive.QUERY)) return new QueryResponse(token,json);
		
		return new ErrorResponse(token,500,jsonResponse);	
	}
	
	protected Response parseSPARQL11SEResponse(String response,SPARQL11SEPrimitive op) {
		if (response == null) return new ErrorResponse(0,500,"Response is null");
		
		JsonObject json = null;
		try {
			json = new JsonParser().parse(response).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			//if (op == SPARQL11SEPrimitive.SECUREUPDATE) return new UpdateResponse(response);
			
			return new ErrorResponse(0,500,"Unknown response: "+response);
		}
		
		//Error response
		if (json.get("code")!=null) 
			if (json.get("code").getAsInt() >= 400) return new ErrorResponse(0,json.get("code").getAsInt(),json.get("body").getAsString());
		
		if (op == SPARQL11SEPrimitive.SECUREQUERY) return new QueryResponse(json);
		if (op == SPARQL11SEPrimitive.SECUREUPDATE) return new UpdateResponse(response);
		
		if (op == SPARQL11SEPrimitive.REGISTER) {
			if (json.get("client_id") != null && json.get("client_secret") != null) {
				properties.setCredentials(json.get("client_id").getAsString(), json.get("client_secret").getAsString());
				
				return new RegistrationResponse(
						json.get("client_id").getAsString(),
						json.get("client_secret").getAsString(),
						json.get("signature"));
			}
			return new ErrorResponse(0,401,"Credentials not found in registration response");	
		}
		
		if (op == SPARQL11SEPrimitive.REQUESTTOKEN) {
			if (json.get("access_token") != null && json.get("expires_in") != null && json.get("token_type") != null){
				int seconds = json.get("expires_in").getAsInt();
				Date expires = new Date();
				expires.setTime(expires.getTime()+(1000*seconds));
				properties.setJWT(json.get("access_token").getAsString(),expires,json.get("token_type").getAsString());
				return new JWTResponse(
						json.get("access_token").getAsString(),
						json.get("token_type").getAsString(),
						json.get("expires_in").getAsLong());
			}
			else if (json.get("code") != null && json.get("body") != null) return new ErrorResponse(0,json.get("code").getAsInt(),json.get("body").getAsString());
			else if (json.get("code") != null) return new ErrorResponse(0,json.get("code").getAsInt(),"");
			
			return new ErrorResponse(0,500,"Response not recognized: "+json.toString());
		}
		
		return new ErrorResponse(0,500,"Response unknown: "+response);	
	}
}
