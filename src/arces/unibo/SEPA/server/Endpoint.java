/* This class implements the SPARQL processing service (endpoint) interface of the Semantic Event Processing Architecture (SEPA) Engine
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
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.response.QueryResponse;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.UpdateResponse;

/**
 * This class implements the SPARQL 1.1 protocol interface
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Endpoint implements EndpointMBean {
	private static String tag ="Endpoint";
	
	private enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	private enum ResultsFormat {JSON,XML,CSV};
	private enum SPARQLOperation {QUERY,UPDATE};
	private static final Logger logger = LogManager.getLogger("Endpoint");
	
	public class SPARQLEndpointProperties {
		private String scheme;
		private String host;
		private int port;
		private String path;
		private HTTPMethod queryMethod;
		private HTTPMethod updateMethod;
		private ResultsFormat resultsFormat;
			
		public String getHttpScheme() {
			return scheme;
		}
		public void setHttpScheme(String endpointHttpScheme) {
			this.scheme = endpointHttpScheme;
		}
		
		public String getHost() {
			return host;
		}
		public void setHost(String endpointHost) {
			this.host = endpointHost;
		}
		
		public int getPort() {
			return port;
		}
		public void setPort(int endpointPort) {
			this.port = endpointPort;
		}
		
		public String getPath() {
			return path;
		}
		
		public void setPath(String endpointPath) {
			this.path = endpointPath;
		}
		public HTTPMethod getQueryMethod() {
			return queryMethod;
		}
		public void setQueryMethod(HTTPMethod endpointQueryMethod) {
			this.queryMethod = endpointQueryMethod;
		}
		public ResultsFormat getQueryResultsFormat() {
			return resultsFormat;
		}
		public void setQueryResultsFormat(ResultsFormat endpointQueryResultsFormat) {
			this.resultsFormat = endpointQueryResultsFormat;
		}
		public void setUpdateMethod(HTTPMethod post) {
			this.updateMethod = post;	
		}
		public HTTPMethod getUpdateMethod() {
			return updateMethod;	
		}
	}
	
	private SPARQLEndpointProperties endpointProperties = new SPARQLEndpointProperties();
	
	private static CloseableHttpClient httpclient = HttpClients.createDefault();
	private static ResponseHandler<String> responseHandler;
	
	public Endpoint(Properties properties) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
				
		//Get the MBean server
	    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	    
	    //register the MBean
	    ObjectName name = new ObjectName("arces.unibo.SEPA.server:type=Endpoint");
	    mbs.registerMBean(this, name);
		
		if (properties == null) logger.error("Properties are null");
		else {
			endpointProperties.setHttpScheme(properties.getProperty("endpointHttpScheme", "http"));
			endpointProperties.setHost(properties.getProperty("endpointHost", "localhost"));
			endpointProperties.setPort(Integer.parseInt(properties.getProperty("endpointPort", "9999")));
			endpointProperties.setPath(properties.getProperty("endpointPath", "/bigdata/sparql"));
			
			switch (properties.getProperty("endpointQueryMethod", "GET")){
				case "GET":
					endpointProperties.setQueryMethod(HTTPMethod.GET);
					break;
				case "POST":
					endpointProperties.setQueryMethod(HTTPMethod.POST);
					break;
				case "URL_ENCODED_POST":
					endpointProperties.setQueryMethod(HTTPMethod.URL_ENCODED_POST);
					break;
			}
			
			switch (properties.getProperty("endpointUpdateMethod", "POST")){
				case "POST":
					endpointProperties.setUpdateMethod(HTTPMethod.POST);
					break;
				case "URL_ENCODED_POST":
					endpointProperties.setUpdateMethod(HTTPMethod.URL_ENCODED_POST);
					break;
			}
			
			switch (properties.getProperty("endpointQueryResultsFormat", "JSON")){
				case "XML":
					endpointProperties.setQueryResultsFormat(ResultsFormat.XML);
					break;
				case "JSON":
					endpointProperties.setQueryResultsFormat(ResultsFormat.JSON);
					break;
				case "CSV":
					endpointProperties.setQueryResultsFormat(ResultsFormat.CSV);
					break;
			}
		}
		
		logger.info("SPARQL endpoint service properties");
		logger.info("Host:" + endpointProperties.getHost());
		logger.info("Port:" + endpointProperties.getPort());
		logger.info("Scheme:" + endpointProperties.getHttpScheme());
		logger.info("Path:" + endpointProperties.getPath());
		logger.info("Query method:" + endpointProperties.getQueryMethod());
		logger.info("Update method:" + endpointProperties.getUpdateMethod());
		logger.info("Query results format:" + endpointProperties.getQueryResultsFormat());
			
		responseHandler = new ResponseHandler<String>() {
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
	            	logger.error("Unexpected response status: " + status);
	            	json.add("status", new JsonPrimitive(false));
                	json.add("body", new JsonPrimitive("Http response entity is null. Response status: "+status));
	            }
	            return json.toString();
	        }
      };
	}
	
	public UpdateResponse update(UpdateRequest req) {
		String response = SPARQLProtocolOperation(req.getSPARQL(),SPARQLOperation.UPDATE,endpointProperties.getUpdateMethod(),endpointProperties.getQueryResultsFormat());
		return new UpdateResponse(req.getToken(),response);
	}

	public Response query(QueryRequest req) {
		switch(endpointProperties.getQueryResultsFormat()) {
			case JSON:
				String response = SPARQLProtocolOperation(req.getSPARQL(),SPARQLOperation.QUERY,endpointProperties.getQueryMethod(),endpointProperties.getQueryResultsFormat());
				
				JsonParser parser = new JsonParser();
				JsonObject json = parser.parse(response).getAsJsonObject();
			
				if (!json.get("status").getAsBoolean()) return new ErrorResponse(req.getToken(),json.get("body").getAsString());
				String ret = json.get("body").getAsString();
				
				return new QueryResponse(req.getToken(),new JsonParser().parse(ret).getAsJsonObject());
			
			default:
				return new ErrorResponse(req.getToken(),"Usupported query result format");
		}
		
	}
	
	private String SPARQLProtocolOperation(String sparql,SPARQLOperation op,HTTPMethod method,ResultsFormat format) {
		String responseBody = null;
		URI uri;
		HttpUriRequest httpRequest;
		String query = null;
		String contentType = null;
		HttpEntity body = null;
		
		JsonObject json = new JsonObject();
		
		if (method.equals(HTTPMethod.POST)) {
			try {
				body = new ByteArrayEntity(sparql.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				json.add("status",new JsonPrimitive(false));
				json.add("body", new JsonPrimitive(e.getMessage()));
				return json.toString();
			}
			if (op.equals(SPARQLOperation.QUERY)) {
				contentType = "application/sparql-query";	
			}
			else contentType = "application/sparql-update";
		}
		
		if (method.equals(HTTPMethod.URL_ENCODED_POST)) {
			contentType = "application/x-www-form-urlencoded";
			String encodedSparql;
			try {
				encodedSparql = URLEncoder.encode(sparql, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				json.add("status",new JsonPrimitive(false));
				json.add("body", new JsonPrimitive(e.getMessage()));
				return json.toString();
			}
			
			if (op.equals(SPARQLOperation.QUERY)) {
				encodedSparql = "query="+encodedSparql;	
			}
			else encodedSparql = "update="+encodedSparql;
			
			try {
				body = new ByteArrayEntity(encodedSparql.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				json.add("status",new JsonPrimitive(false));
				json.add("body", new JsonPrimitive(e.getMessage()));
				return json.toString();
			}
		}
		
		if (method.equals(HTTPMethod.GET)){
			if (!op.equals(SPARQLOperation.QUERY)) {
				json.add("status",new JsonPrimitive(false));
				json.add("body", new JsonPrimitive("GET method can be used only for query operation"));
				return json.toString();
			}
			try {
				query = "query="+URLEncoder.encode(sparql, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				json.add("status",new JsonPrimitive(false));
				json.add("body", new JsonPrimitive(e.getMessage()));
				return json.toString();
			}
		}
		
		try {
			uri = new URI(endpointProperties.getHttpScheme(),
					   null,
					   endpointProperties.getHost(),
					   endpointProperties.getPort(),
					   endpointProperties.getPath(),
					   query,
					   null);
		} catch (URISyntaxException e) {
			logger.error("Error on creating request URI "+e.getMessage());
			json.add("status",new JsonPrimitive(false));
			json.add("body", new JsonPrimitive(e.getMessage()));
			return json.toString();
		}
		
		//GET or POST
		if (method.equals(HTTPMethod.GET)) {
			httpRequest = new HttpGet(uri);	 	
		}
		else {
			httpRequest = new HttpPost(uri);
		}
		
		//HTTP Header to specify the results format
		switch(format){
			case XML:
				httpRequest.setHeader("Accept", "application/sparql-results+xml");
				break;
			case JSON:
				httpRequest.setHeader("Accept", "application/sparql-results+json");
				break;
			default:
				break;
		}
		
		//Content-Type
		if (contentType != null) httpRequest.setHeader("Content-Type", contentType);
		if (body != null) ((HttpPost) httpRequest).setEntity(body);
		
		try {
			long timing = System.nanoTime();
	    	
			responseBody = httpclient.execute(httpRequest, responseHandler);
	    	
			timing = System.nanoTime() - timing;
	    	
			if(op.equals(SPARQLOperation.QUERY)) logger.info("Query "+timing+ " ns");
			else logger.info("Update "+timing+ " ns");
	    }
	    catch(java.net.ConnectException e) {
	    	logger.error(e.getMessage());
	    	json.add("status",new JsonPrimitive(false));
			json.add("body", new JsonPrimitive(e.getMessage()));
			return json.toString();
	    } 
		catch (ClientProtocolException e) {
			logger.error(e.getMessage());	
			json.add("status",new JsonPrimitive(false));
			json.add("body", new JsonPrimitive(e.getMessage()));
			return json.toString();
		} 
		catch (IOException e) {
			logger.error(e.getMessage());
			json.add("status",new JsonPrimitive(false));
			json.add("body", new JsonPrimitive(e.getMessage()));
			return json.toString();
		}
		return responseBody;
	}

	@Override
	public String getHost() {
		return this.endpointProperties.getHost();
	}

	@Override
	public int getPort() {
		return this.endpointProperties.getPort();
	}

	@Override
	public String getPath() {
		return this.endpointProperties.getPath();
	}
}
