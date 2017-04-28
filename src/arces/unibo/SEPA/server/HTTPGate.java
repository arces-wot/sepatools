/* This class implements the W3C SPARQL 1.1 Protocol 
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
import java.io.OutputStream;

import java.net.InetSocketAddress;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import arces.unibo.SEPA.beans.HTTPGateMBean;
import arces.unibo.SEPA.beans.SEPABeans;
import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseAndNotificationListener;

/**
 * This class implements the SPARQL 1.1 Protocol 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class HTTPGate extends Thread implements HTTPGateMBean {
	
	protected static HttpServer server = null;
	protected Logger logger = LogManager.getLogger("HTTP Gate");	
	protected static String mBeanName = "arces.unibo.SEPA.server:type=HTTPGate";
	
	private static int port = 8000; 
	private static int timeout = 2000;

	private Scheduler scheduler;
	protected long transactions  = 0; 
	private long updateTransactions  = 0;
	private long queryTransactions  = 0;
	
	public HTTPGate(EngineProperties properties,Scheduler scheduler) {
		if (properties == null) logger.error("Properties are null");
		else {
			timeout = properties.getHttpTimeout();
			port = properties.getHttpPort();
		}
			
		this.scheduler = scheduler;
		if (scheduler == null) logger.error("Scheduler is null");	
		
		SEPABeans.registerMBean(this,mBeanName);
	}

	@Override
	public void run() {
		
		try {
			server.wait();
		} catch (InterruptedException e) {
			logger.info(e.getMessage());
		}
		
	}
	
	@Override
	public void start(){	
		this.setName("SEPA HTTP Gate");
		   
		try 
		{
			server = HttpServer.create(new InetSocketAddress(port), 0);
		} 
		catch (IOException e) {
			e.printStackTrace();
			logger.fatal(e.getMessage());
			System.exit(1);
		}
		
	    server.createContext("/sparql", new SPARQLHandler());
	    server.createContext("/echo", new EchoHandler()); 
	    server.setExecutor(null);
	    
	    server.start();
	    
	    logger.info("Started on port "+port);
	}
	
	@Override
	public void interrupt(){
		logger.info("Kill signal received...stopping HTTP server...");
		if (server != null) server.stop(0);
		super.interrupt();
	}
	
	private void echoRequest(HttpExchange exchange) {
		String response = 	"METHOD: " + exchange.getRequestMethod().toUpperCase() + "\n";
		response += 	  	"PROTOCOL: " + exchange.getProtocol() + "\n";
		
		String headerString = "";
		for (String header : exchange.getRequestHeaders().keySet()) {
			headerString += " " + header + ":" + exchange.getRequestHeaders().get(header).toString() +"\n";
		}
		response +=       	"HEADERS:\n" + headerString;
		try {
			response +=       	"BODY:\n" + IOUtils.toString(exchange.getRequestBody(),"UTF-8") +"\n";
		} catch (IOException e) {
			logger.error("Error on UTF-8 decoding "+e.getMessage());
		}
		response += 		"CONTEXT PATH: " + exchange.getHttpContext().getPath() + "\n";
		response += 		"QUERY: " + exchange.getRequestURI().getQuery();
		
		try {
			exchange.sendResponseHeaders(200, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (IOException e) {
			logger.error("Error on sending HTTP response "+e.getMessage());
		}
			
	}
	
	protected void failureResponse(HttpExchange exchange,int httpResponseCode,String responseBody) {
		String response = 	"Response\n"+responseBody + "\n\nRequest\nMETHOD: " + exchange.getRequestMethod().toUpperCase() + "\n";
		response += 	  	"PROTOCOL: " + exchange.getProtocol() + "\n";
		String headerString = "";
		for (String header : exchange.getRequestHeaders().keySet()) {
			headerString += " " + header + ":" + exchange.getRequestHeaders().get(header).toString() +"\n";
		}
		response +=       	"HEADERS:\n" + headerString;
		try {
			response +=       	"BODY:\n" + IOUtils.toString(exchange.getRequestBody(),"UTF-8") +"\n";
		} catch (IOException e) {
			logger.error("Error on UTF-8 decoding "+e.getMessage());
		}
		response += 		"CONTEXT PATH: " + exchange.getHttpContext().getPath() + "\n";
		response += 		"QUERY: " + exchange.getRequestURI().getQuery();
		
		sendResponse(exchange,httpResponseCode,response);
	}
	
	protected void sendResponse(HttpExchange exchange,int httpResponseCode,String response){
		//TODO: CORS to be checked
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "X-Requested-With,Content-Type,Origin,Accept");
		
		try {
			exchange.sendResponseHeaders(httpResponseCode, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (IOException e) {
			logger.error("Error on sending HTTP response "+e.getMessage());
		}	
	}
	
	public class EchoHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			echoRequest(exchange);
		}
	}
	
	/**
	 * This method parse the HTTP request according to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)
	 * 
	 * @return  the corresponding request (update or query), otherwise null
	 * @throws IOException 
	 * 
	 * @see QueryRequest, UpdateRequest
	* */
	private Request parseSPARQL11(HttpExchange httpExchange) {
		
		switch(httpExchange.getRequestMethod().toUpperCase()) {
			case "GET":
				logger.debug("HTTP GET");
				if (httpExchange.getRequestURI().getQuery()== null) {
					failureResponse(httpExchange,400,"query is null");
					return null;	
				}
				String[] query = httpExchange.getRequestURI().getQuery().split("&");
				for (String param : query) {
					String[] value = param.split("=");
					if (value[0].equals("query")) {
						this.queryTransactions++;
						return new QueryRequest(scheduler.getToken(),value[1]);
					}
				}
				failureResponse(httpExchange,400,"Query must be in the form: \"query=<SPARQL 1.1 Query>\"");
				return null;	
			
			case "POST":
				logger.debug("HTTP POST");

				String sparql = null;
				try {
					sparql = IOUtils.toString(httpExchange.getRequestBody(),"UTF-8");
				} catch (IOException e) {
					logger.error("Exception on reading SPARQL from POST body: "+e.getMessage());
					failureResponse(httpExchange,400,"Exception on reading SPARQL from POST body");
					return null;
				}
				if(httpExchange.getRequestHeaders().get("Content-Type").contains("application/sparql-query")) {
					this.queryTransactions++;
					return new QueryRequest(scheduler.getToken(),sparql);
				}
				if(httpExchange.getRequestHeaders().get("Content-Type").contains("application/sparql-update")) {
					this.updateTransactions++;
					return new UpdateRequest(scheduler.getToken(),sparql);
				}

				if(httpExchange.getRequestHeaders().get("Content-Type").contains("application/x-www-form-urlencoded")) {
					if (sparql.contains("query=")){
						this.queryTransactions++;
						return new QueryRequest(scheduler.getToken(),sparql);
					}
					if (sparql.contains("update="))
						this.updateTransactions++;
						return new UpdateRequest(scheduler.getToken(),sparql);
				}
									
				logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
				failureResponse(httpExchange,400,"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
				return null;
				
			case "OPTIONS":
				// Debug print
				logger.debug("HTTP OPTIONS");
				
			    // ok, we are ready to send the response.
			    try {
			    	//TODO: CORS to be checked
					httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
					httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT");
					httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "X-Requested-With,Content-Type,Origin,Accept");
					
			    	httpExchange.sendResponseHeaders(200, 0);
			    	OutputStream os = httpExchange.getResponseBody();			    
				    os.write(0);
				    os.close();
			    } catch (IOException e) {
			    	logger.error(e.getMessage());
			    }			    

				return null;
		}		
		
		logger.error("UNSUPPORTED METHOD: "+httpExchange.getRequestMethod().toUpperCase());		
		failureResponse(httpExchange,400,"Unsupported method: "+httpExchange.getRequestMethod());
			
		return null;
	}
	
	private boolean validate(Request request) {
		// TODO to be implemented
		return true;
	}
	
	public class SPARQLHandler implements HttpHandler  {		
		class Running extends Thread implements ResponseAndNotificationListener {
			private HttpExchange httpExchange;
			private Response response = null;
			
			public Running(HttpExchange httpExchange) {
				this.httpExchange = httpExchange;
			}
			
			public void run() {
				
				//Parsing SPARQL 1.1 request
				Request request = parseSPARQL11(httpExchange);

				//Timestamp
				long startTime = System.nanoTime();
				
				//Validate
				if (!validate(request)) {
					logger.error("SPARQL 1.1 validation failed "+request.getSPARQL());		
					failureResponse(httpExchange,400,"SPARQL 1.1 validation failed "+request.getSPARQL());
					return;
				}
				
				//Timestamp
				long validatedTime = System.nanoTime();
				
				//Add request
				if (request != null) scheduler.addRequest(request,this);
				else return;
								
				//Waiting response
				logger.debug("Waiting response in "+timeout+" ms...");
				
				try { Thread.sleep(timeout);} 
				catch (InterruptedException e) {}
				
				//Timestamp
				long processedTime = System.nanoTime();
				
				//Logging
				logger.trace("Request validated in " + (startTime-validatedTime) + " and processed in " + (validatedTime-processedTime) + " ns");
				
				//Send HTTP response
				if (response == null) sendResponse(httpExchange,408,"Timeout");
				else {
					// Check response status
					JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
					
					//Query or update response
					sendResponse(httpExchange,json.get("code").getAsInt(),json.get("body").toString());
				}
				
				scheduler.releaseToken(request.getToken()); 
			}
			
			@Override
			public void notify(Response response) {
				logger.info("Response #"+response.getToken());
				this.response = response;
				interrupt();
			}
		}
		
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			logger.info(">> HTTP request");
			transactions += 1;
			new Running(httpExchange).start();
		}
	}

	@Override
	public long getTransactions() {
		return this.transactions;
	}

	@Override
	public long getQueryTransactions() {
		return this.queryTransactions;
	}

	@Override
	public long getUpdateTransactions() {
		return this.updateTransactions;
	}
}
