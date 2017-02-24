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
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.*;

import arces.unibo.SEPA.application.SEPALogger;
import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
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

public class HTTPGate extends Thread {
	
	private String tag ="HTTPGate";

	private static HttpServer server = null;
	
	private static int httpPort = 8000; 
	private static int httpTimeout = 2000;

	private Scheduler scheduler;
	
	public HTTPGate(Properties properties,Scheduler scheduler){
		if (properties == null) SEPALogger.log(VERBOSITY.ERROR, tag, "Properties are null");
		else {
			httpTimeout = Integer.parseInt(properties.getProperty("httpTimeout", "2000"));
			httpPort = Integer.parseInt(properties.getProperty("httpPort", "8000"));
		}
			
		this.scheduler = scheduler;
		if (scheduler == null) SEPALogger.log(VERBOSITY.ERROR, tag, "Scheduler is null");
	
	}

	@Override
	public void run() {
		try {
			server.wait();
		} catch (InterruptedException e) {
			SEPALogger.log(VERBOSITY.INFO, tag, e.getMessage());
		}
		
	}
	
	@Override
	public void start(){	
		this.setName("SEPA HTTP Gate");
		try 
		{
			server = HttpServer.create(new InetSocketAddress(httpPort), 0);
		} 
		catch (IOException e) {
			e.printStackTrace();
			SEPALogger.log(VERBOSITY.FATAL,tag ,e.getMessage());
			return;
		}
		
	    server.createContext("/sparql", new SPARQLHandler());
	    server.createContext("/echo", new EchoHandler()); 
	    server.setExecutor(null);
	    server.start();
	    
	    SEPALogger.log(VERBOSITY.INFO, tag, "Started on port "+httpPort);
	}
	
	@Override
	public void interrupt(){
		SEPALogger.log(VERBOSITY.INFO, tag, "Kill signal received...stopping HTTP server...");
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
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on UTF-8 decoding "+e.getMessage());
		}
		response += 		"CONTEXT PATH: " + exchange.getHttpContext().getPath() + "\n";
		response += 		"QUERY: " + exchange.getRequestURI().getQuery();
		
		try {
			exchange.sendResponseHeaders(200, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on sending HTTP response "+e.getMessage());
		}
			
	}
	
	private void failureResponse(HttpExchange exchange,int httpResponseCode,String responseBody) {
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
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on UTF-8 decoding "+e.getMessage());
		}
		response += 		"CONTEXT PATH: " + exchange.getHttpContext().getPath() + "\n";
		response += 		"QUERY: " + exchange.getRequestURI().getQuery();
		
		try {
			exchange.sendResponseHeaders(httpResponseCode, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on sending HTTP response "+e.getMessage());
		}
	}
	
	class EchoHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			echoRequest(exchange);
		}
	}
	
	/**
	 * This method parse the HTTP request according to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)
	 * 
	 * @return  the corresponding request (update or query), otherwise null
	 * 
	 * @see QueryRequest, UpdateRequest
	* */
	private Request parseSPARQL11(HttpExchange httpExchange) {
		
		switch(httpExchange.getRequestMethod().toUpperCase()) {
			case "GET":
				SEPALogger.log(VERBOSITY.DEBUG,tag,"HTTP GET");
				if (httpExchange.getRequestURI().getQuery()== null) {
					failureResponse(httpExchange,500,"query is null");
					return null;	
				}
				String[] query = httpExchange.getRequestURI().getQuery().split("&");
				for (String param : query) {
					String[] value = param.split("=");
					if (value[0].equals("query")) return new QueryRequest(scheduler.getToken(),value[1]);
				}
				failureResponse(httpExchange,400,"Query must be in the form: \"query=<SPARQL 1.1 Query>\"");
				return null;	
			
			case "POST":
				SEPALogger.log(VERBOSITY.DEBUG,tag,"HTTP POST");
				String sparql = null;
				try {
					sparql = IOUtils.toString(httpExchange.getRequestBody(),"UTF-8");
				} catch (IOException e) {
					SEPALogger.log(VERBOSITY.ERROR, tag, "Exception on reading SPARQL from POST body: "+e.getMessage());
					failureResponse(httpExchange,400,"Exception on reading SPARQL from POST body");
					return null;
				}
				if(httpExchange.getRequestHeaders().get("Content-Type").contains("application/sparql-query")) return new QueryRequest(scheduler.getToken(),sparql);
				if(httpExchange.getRequestHeaders().get("Content-Type").contains("application/sparql-update")) return new UpdateRequest(scheduler.getToken(),sparql);

				if(httpExchange.getRequestHeaders().get("Content-Type").contains("application/x-www-form-urlencoded")) {
					if (sparql.contains("query="))
						return new QueryRequest(scheduler.getToken(),sparql);
					if (sparql.contains("update="))
						return new UpdateRequest(scheduler.getToken(),sparql);
				}
									
				SEPALogger.log(VERBOSITY.ERROR, tag,"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
				failureResponse(httpExchange,400,"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
				return null;
		}
		
		SEPALogger.log(VERBOSITY.ERROR,tag,"UNSUPPORTED METHOD: "+httpExchange.getRequestMethod().toUpperCase());		
		failureResponse(httpExchange,400,"Unsupported method: "+httpExchange.getRequestMethod());
			
		return null;
	}
	
	private boolean validate(Request request) {
		// TODO to be implemented
		return true;
	}
	
	class SPARQLHandler implements HttpHandler  {		
		class Running extends Thread implements ResponseAndNotificationListener {
			private HttpExchange httpExchange;
			private Response response = null;
			
			public Running(HttpExchange httpExchange) {
				this.httpExchange = httpExchange;
			}
			
			public void run() {
				//Parsing SPARQL 1.1 request
				Request request = parseSPARQL11(httpExchange);
				
				//Validate
				if (!validate(request)) {
					SEPALogger.log(VERBOSITY.ERROR,tag,"SPARQL 1.1 validation failed "+request.getSPARQL());		
					failureResponse(httpExchange,400,"SPARQL 1.1 validation failed "+request.getSPARQL());
					return;
				}
				
				//Add request
				if (request != null) scheduler.addRequest(request,this);
				else return;
								
				//Waiting response
				SEPALogger.log(VERBOSITY.DEBUG, tag,"Waiting response in "+httpTimeout+" ms...");
				
				try { Thread.sleep(httpTimeout);} 
				catch (InterruptedException e) {}
				
				//Send HTTP response
				sendResponse(request.getToken());
				
				scheduler.releaseToken(request.getToken()); 
			}
			
			private void sendResponse(Integer token) {
				try 
				{
					if (response == null) {
						String error = "Timeout";
						httpExchange.sendResponseHeaders(500, error.length());
						OutputStream os = httpExchange.getResponseBody();
						os.write(error.getBytes());
						os.close();
					}
					else {
						byte[] responseBody = response.toString().getBytes();
						SEPALogger.log(VERBOSITY.DEBUG, tag, "Send HTTP response of "+responseBody.length+ " bytes");
						httpExchange.sendResponseHeaders(200, responseBody.length);
						OutputStream os = httpExchange.getResponseBody();
						os.write(responseBody);
						os.close();
					}
					SEPALogger.log(VERBOSITY.INFO, tag, "<< HTTP response");
				} 
				catch (IOException e) {
					SEPALogger.log(VERBOSITY.FATAL,tag,"Send HTTP Response failed ");
				}	
			}

			@Override
			public void notify(Response response) {
				SEPALogger.log(VERBOSITY.INFO, tag, "Response #"+response.getToken());
				this.response = response;
				interrupt();
			}
		}
		
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			SEPALogger.log(VERBOSITY.INFO, tag, ">> HTTP request");
			new Running(httpExchange).start();
		}
	}
}
