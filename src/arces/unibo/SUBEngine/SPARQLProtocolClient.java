package arces.unibo.SUBEngine;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Properties;

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

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;

/**
 * This class implements SPARQL 1.1 protocol client
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class SPARQLProtocolClient {
	private static String tag ="Endpoint";
	
	private enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	private enum ResultsFormat {JSON,XML,CSV};
	private enum SPARQLOperation {QUERY,UPDATE};
	
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
	
	public SPARQLProtocolClient(Properties properties) {
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
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
		
		responseHandler = new ResponseHandler<String>() {
	        @Override
	        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
	            int status = response.getStatusLine().getStatusCode();
	            if (status >= 200 && status < 300) 
	            {
	                HttpEntity entity = response.getEntity();
	                return entity != null ? EntityUtils.toString(entity) : "ERROR "+status;
	            } 
	            else 
	            {
	            	Logger.log(VERBOSITY.ERROR, tag, "Unexpected response status: " + status);
	            	return "ERROR "+status;
	            }
	        }
      };
	}
	
	public UpdateResponse update(UpdateRequest req) {
		String response = SPARQLProtocolOperation(req.getSPARQL(),SPARQLOperation.UPDATE,endpointProperties.getUpdateMethod(),endpointProperties.getQueryResultsFormat());
		return new UpdateResponse(req.getToken(),response);
	}

	public QueryResponse query(QueryRequest req) {
		String response = SPARQLProtocolOperation(req.getSPARQL(),SPARQLOperation.QUERY,endpointProperties.getQueryMethod(),endpointProperties.getQueryResultsFormat());
		return new QueryResponse(req.getToken(),response);
	}
	
	private String SPARQLProtocolOperation(String sparql,SPARQLOperation op,HTTPMethod method,ResultsFormat format) {
		String responseBody = null;
		URI uri;
		HttpUriRequest httpRequest;
		String query = null;
		String contentType = null;
		HttpEntity body = null;
		
		if (method.equals(HTTPMethod.POST)) {
			try {
				body = new ByteArrayEntity(sparql.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Logger.log(VERBOSITY.ERROR, tag, e.getMessage());
				return "ERROR "+e.getMessage(); 
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
				return "ERROR "+e.getMessage();
			}
			
			if (op.equals(SPARQLOperation.QUERY)) {
				encodedSparql = "query="+encodedSparql;	
			}
			else encodedSparql = "update="+encodedSparql;
			
			try {
				body = new ByteArrayEntity(encodedSparql.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Logger.log(VERBOSITY.ERROR, tag, e.getMessage());
				return "ERROR "+e.getMessage(); 
			}
		}
		
		if (method.equals(HTTPMethod.GET)){
			if (!op.equals(SPARQLOperation.QUERY)) {
				return "ERROR GET method can be used only for query operation";
			}
			try {
				query = "query="+URLEncoder.encode(sparql, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return "ERROR "+e.getMessage();
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
			Logger.log(VERBOSITY.ERROR, tag, "Error on creating request URI "+e.getMessage());
			return "Error on creating request URI "+e.getMessage();
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
	    	Logger.log(VERBOSITY.INFO, "timing", "QueryTime "+timing+ " ns");
	    }
	    catch(java.net.ConnectException e) {
	    	Logger.log(VERBOSITY.ERROR, tag, e.getMessage());
	      	return "ERROR " + e.getMessage();
	    } 
		catch (ClientProtocolException e) {
			Logger.log(VERBOSITY.ERROR, tag, e.getMessage());	
	    	return "ERROR " + e.getMessage();
		} 
		catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, e.getMessage());
			return "ERROR " + e.getMessage();
		}
		return responseBody; 	
	}
}
