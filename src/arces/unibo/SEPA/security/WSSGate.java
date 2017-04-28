/* This class implements the SPARQL Secure Event (SE) 1.1 Protocol over TLS
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

package arces.unibo.SEPA.security;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;

import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import arces.unibo.SEPA.server.EngineProperties;
import arces.unibo.SEPA.server.Scheduler;
import arces.unibo.SEPA.server.WSGate;

public class WSSGate extends WSGate {
	private Logger logger = LogManager.getLogger("WSSGate");
	protected static String mBeanName = "arces.unibo.SEPA.server:type=WSSGate";
	
	//Security context and manager
	private SecurityManager sManager = new SecurityManager();
	private int wssPort = 9443;
	
	//Authorization manager
	private static AuthorizationManager am = new AuthorizationManager();
		
	public WSSGate(EngineProperties properties, Scheduler scheduler)  {
		super(properties, scheduler);
		
		if (scheduler == null) logger.error("Scheduler is null");
		this.scheduler = scheduler;
		
		if (properties == null) logger.error("Properties are null");
		else {
			wssPort = properties.getWssPort();
		}
        
        logger.debug("Created");
	}
	
	public boolean start(){
		//Create a secure Websocket application
		final HttpServer secureServer = HttpServer.createSimpleServer("/var/www/wss",wssPort);
		secureServer.getListener("grizzly").registerAddOn(new WebSocketAddOn());
		secureServer.getListener("grizzly").setSSLEngineConfig(sManager.getWssConfigurator());
		secureServer.getListener("grizzly").setSecure(true);
		
		  // Register the application
        WebSocketEngine.getEngine().register("", "/sparql", this);
        
        try {
        	secureServer.start();
		} catch (IOException e) {
			logger.fatal("Failed to start Secure WebSocket gate on port "+wssPort+ " "+e.getMessage());
			System.exit(1);
		}
		
		logger.info("Started on port "+wssPort);

		if (keepAlivePeriod > 0) {
			new KeepAlive().start();
		}
		return true;
	}
	
	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		logger.info("onClose");
		super.onClose(socket, frame);
	}
	
	@Override
	public void onConnect(WebSocket socket) {
		logger.info("onConnect");
		super.onConnect(socket);
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		if (validateToken(text)) super.onMessage(socket, text);
		else {
			//Not authorized
			JsonObject error = new JsonObject();
			error.add("error", new JsonPrimitive("Authorization missing or token not valid"));
			socket.send(error.toString());
		}
	}

	/* SPARQL 1.1 Subscribe language 
	 * 
	 * {"subscribe":"SPARQL Query 1.1", "authorization": "JWT"}
	 * 
	 * {"subscribe":"SPUID", "authorization": "JWT"}
	 * 
	 * In not secure connections (ws), authorization key can be missing
	 * */
	private boolean validateToken(String request) {
		JsonObject req;
		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			return false;
		}
		
		if (req.get("authorization") == null) return false;
		
		//Token validation
		
		 return am.validateToken(req.get("authorization").getAsString()).get("valid").getAsBoolean();
	}
}
