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

package arces.unibo.SEPA.commons.SPARQL;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.beans.EndpointMBean;
import arces.unibo.SEPA.beans.SEPABeans;
import arces.unibo.SEPA.commons.SPARQL.ProtocolProperties.HTTPMethod;
import arces.unibo.SEPA.commons.SPARQL.ProtocolProperties.ResultsFormat;
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
		
	private static final Logger logger = LogManager.getLogger("Endpoint");
	protected static String mBeanName = "arces.unibo.SEPA.server:type=Endpoint";
	
	private EndpointProperties endpointProperties;
	private enum SPARQLOperation {QUERY,UPDATE};
	
	private CloseableHttpClient httpclient = HttpClients.createDefault();
	private ResponseHandler<String> responseHandler;
	
	public String toString() {
		return endpointProperties.toString();
	}
	
	public Endpoint(EndpointProperties properties) {
				
		SEPABeans.registerMBean(this,mBeanName);
		
		if (properties == null) {
			logger.error("Properties are null");
			System.exit(1);
		}
		else {
			endpointProperties = properties;
		}
		
		responseHandler = new ResponseHandler<String>() {
	        @Override
	        public String handleResponse(final HttpResponse response) {
	            /*SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)
	        	
	        	UPDATE
	        	 2.2 update operation
	        	 The response to an update request indicates success or failure of the request via HTTP response status code.
	        	
	        	QUERY
	        	 2.1.5 Accepted Response Formats

				Protocol clients should use HTTP content negotiation [RFC2616] to request response formats that the client can consume. See below for more on potential response formats.
				
				2.1.6 Success Responses
				
				The SPARQL Protocol uses the response status codes defined in HTTP to indicate the success or failure of an operation. Consult the HTTP specification [RFC2616] for detailed definitions of each status code. While a protocol service should use a 2XX HTTP response code for a successful query, it may choose instead to use a 3XX response code as per HTTP.
				
				The response body of a successful query operation with a 2XX response is either:
				
				a SPARQL Results Document in XML, JSON, or CSV/TSV format (for SPARQL Query forms SELECT and ASK); or,
				an RDF graph [RDF-CONCEPTS] serialized, for example, in the RDF/XML syntax [RDF-XML], or an equivalent RDF graph serialization, for SPARQL Query forms DESCRIBE and CONSTRUCT).
				The content type of the response to a successful query operation must be the media type defined for the format of the response body.
				
				2.1.7 Failure Responses
				
				The HTTP response codes applicable to an unsuccessful query operation include:
				
				400 if the SPARQL query supplied in the request is not a legal sequence of characters in the language defined by the SPARQL grammar; or,
				500 if the service fails to execute the query. SPARQL Protocol services may also return a 500 response code if they refuse to execute a query. This response does not indicate whether the server may or may not process a subsequent, identical request or requests.
				The response body of a failed query request is implementation defined. Implementations may use HTTP content negotiation to provide human-readable or machine-processable (or both) information about the failed query request.
				
				A protocol service may use other 4XX or 5XX HTTP response codes for other failure conditions, as per HTTP.
	        	*/
	        	int status = response.getStatusLine().getStatusCode();

	        	JsonObject json = new JsonObject();
	            json.add("code", new JsonPrimitive(status));
	            
	        	String body = null;
	        	
	        	HttpEntity entity = response.getEntity();
	        	
	            try {
					body = EntityUtils.toString(entity,Charset.forName("UTF-8"));
				} catch (ParseException e) {
					body = e.getMessage();
				} catch (IOException e) {
					body = e.getMessage();
				}

	            json.add("body",new JsonPrimitive(body));
	            
	            return json.toString();
	        }
      };
	}
	
	public Response update(UpdateRequest req) {
		return SPARQLProtocolOperation(req.getToken(),req.getSPARQL(),SPARQLOperation.UPDATE,endpointProperties.getUpdateMethod(),endpointProperties.getQueryResultsFormat());
	}

	public Response query(QueryRequest req) {
		return SPARQLProtocolOperation(req.getToken(),req.getSPARQL(),SPARQLOperation.QUERY,endpointProperties.getQueryMethod(),endpointProperties.getQueryResultsFormat());	
	}
	
	private Response SPARQLProtocolOperation(int token,String sparql,SPARQLOperation op,HTTPMethod method,ResultsFormat format) {
		String response = null;
		
		//HTTP request build
		HttpUriRequest httpRequest =  buildRequest(op,method,format,sparql);
		if (httpRequest == null) return new ErrorResponse(token,"Error on building HTTP request URI",414);
		
		//HTTP request execution
		try {
			long timing = System.nanoTime();

			response = httpclient.execute(httpRequest, responseHandler);
	    	
			timing = System.nanoTime() - timing;
	    	
			if(op.equals(SPARQLOperation.QUERY)) logger.info("Query "+timing+ " ns");
			else logger.info("Update "+timing+ " ns");
	    }
	    catch(java.net.ConnectException e) {
	    	logger.error(e.getMessage());
	    	return new ErrorResponse(token,e.getMessage(),503);
	    } 
		catch (ClientProtocolException e) {
			logger.error(e.getMessage());	
			return new ErrorResponse(token,e.getMessage(),500);
		} 
		catch (IOException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(token,e.getMessage(),500);
		}
		
		//Parsing the response (could be SPARQL 1.1 service specific)
		return parseEndpointResponse(token,response,op,format);
	}
	
	private HttpUriRequest buildRequest(SPARQLOperation op,HTTPMethod method,ResultsFormat format,String sparql) {
		URI uri;
		HttpUriRequest httpRequest;
		String query = null;
		String contentType = null;
		StringEntity body = null;
		String accept = null;
		
		/*
	 								HTTP Method			Query String Parameters			Request Content Type				Request Message Body
	 	----------------------------------------------------------------------------------------------------------------------------------------
	 	query via GET				GET					query (exactly 1)				None								None
	 													default-graph-uri (0 or more)
	 													named-graph-uri (0 or more)
	 	----------------------------------------------------------------------------------------------------------------------------------------												
	 	query via URL-encoded POST	POST				None							application/x-www-form-urlencoded	URL-encoded, ampersand-separated query parameters.
	 																														query (exactly 1)
	 																														default-graph-uri (0 or more)
	 																														named-graph-uri (0 or more)
	 	----------------------------------------------------------------------------------------------------------------------------------------																													
	 	query via POST directly		POST				default-graph-uri (0 or more)
	 													named-graph-uri (0 or more)		application/sparql-query			Unencoded SPARQL query string
	 	*/
		//QUERY
		if (op.equals(SPARQLOperation.QUERY)) {
			//Support only the JSON query response format (https://www.w3.org/TR/sparql11-results-json/)
			if(!ResultsFormat.JSON.equals(format)) return null;
			accept = "application/sparql-results+json";
			
			switch (method) {
				case GET:
				try {
					query = "query="+ URLEncoder.encode(sparql, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return null;
				}
					break;
				case URL_ENCODED_POST:
					contentType = "application/x-www-form-urlencoded";
					String encodedSparql;
					try {
						encodedSparql = URLEncoder.encode(sparql, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						logger.error(e.getMessage());
						return null;
					}
					encodedSparql = "query="+encodedSparql;	
					
					body = new StringEntity(encodedSparql,"UTF-8");
					break;
				case POST:
					contentType = "application/sparql-query";	
					body =new StringEntity(sparql,"UTF-8");
					break;
			}
		}
		
		/*
									HTTP Method			Query String Parameters			Request Content Type				Request Message Body
		----------------------------------------------------------------------------------------------------------------------------------------
		update via URL-encoded POST	POST				None							application/x-www-form-urlencoded	URL-encoded, ampersand-separated query parameters.
																															update (exactly 1)
																															using-graph-uri (0 or more)
																															using-named-graph-uri (0 or more)
		----------------------------------------------------------------------------------------------------------------------------------------																													
		update via POST directly	POST				using-graph-uri (0 or more)		application/sparql-update			Unencoded SPARQL update request string
														using-named-graph-uri (0 or more)		
		 */
		//UPDATE
		if (op.equals(SPARQLOperation.UPDATE)) {
			accept = "application/json";
			
			switch (method) {
				case URL_ENCODED_POST:
					contentType = "application/x-www-form-urlencoded";
					String encodedSparql;
					try {
						encodedSparql = URLEncoder.encode(sparql, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						logger.error(e.getMessage());
						return null;
					}
					//encodedSparql = "update="+encodedSparql;	
					
					body = new StringEntity(encodedSparql,"UTF-8");
					break;
				case POST:
					contentType = "application/sparql-update";	
					body =new StringEntity(sparql,"UTF-8");
					break;
				default:
					return null;
			}	
		}
		
		//Path MAY be different for query and update
		String path;
		if (op.equals(SPARQLOperation.QUERY)) path = endpointProperties.getQueryPath();
		else path = endpointProperties.getUpdatePath();
		
		try {
			uri = new URI(endpointProperties.getHttpScheme(),
					   null,
					   endpointProperties.getHost(),
					   endpointProperties.getHttpPort(),
					   path,
					   query,
					   null);
		} catch (URISyntaxException e) {
			logger.error("Error on creating request URI "+e.getMessage());
			return null;
		}
		
		//GET or POST
		if (method.equals(HTTPMethod.GET)) httpRequest = new HttpGet(uri);	 	
		else httpRequest = new HttpPost(uri);
		
		//Headers
		if (contentType != null) httpRequest.setHeader("Content-Type", contentType);
		if (accept != null) httpRequest.setHeader("Accept", accept);
		
		//Request body
		if (body != null) ((HttpPost) httpRequest).setEntity(body);
		
		return httpRequest;
	}
	
	/*It is returned as JSON object
	 * 
	 * {"code":HTTP Status code,
	 * "body": "response body"}
	 * */
	private Response parseEndpointResponse(int token,String jsonResponse,SPARQLOperation op,ResultsFormat format) {
		JsonObject json = new JsonParser().parse(jsonResponse).getAsJsonObject();
		int code = json.get("code").getAsInt();
		String body = json.get("body").getAsString();
		
		if (code != 200) return new ErrorResponse(token,body,code);
		
		if (format.equals(ResultsFormat.JSON) && op.equals(SPARQLOperation.QUERY)) return new QueryResponse(token,body);
		if(op.equals(SPARQLOperation.UPDATE)) return new UpdateResponse(token,body);			
			
		return new ErrorResponse(token,body,415);
	}

	@Override
	public String getHost() {
		return this.endpointProperties.getHost();
	}

	@Override
	public int getPort() {
		return this.endpointProperties.getHttpPort();
	}

	@Override
	public String getQueryPath() {
		return this.endpointProperties.getQueryPath();
	}
	
	@Override
	public String getUpdatePath() {
		return this.endpointProperties.getUpdatePath();
	}
}
