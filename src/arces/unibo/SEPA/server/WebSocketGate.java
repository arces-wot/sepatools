/* This class implements the SPARQL Secure Event (SE) 1.1 Protocol 
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
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import arces.unibo.SEPA.application.SEPALogger;
import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.Ping;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseAndNotificationListener;

/**
 * This class implements the SPARQL 1.1 Secure Event (SE) Protocol to handle Subscribe and Unsubscribe primitives
 * 
 * This is based on Websockets Protocol RFC 6455
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class WebSocketGate extends WebSocketApplication implements WebSocketGateMBean {//implements ResponseListener {
	private String tag ="WebSocketGate";
	
	private Scheduler scheduler;
	
	private int wsPort = 9000;
	private int keepAlivePeriod = 5000;
	
	private HashMap<WebSocket,SEPAResponseListener> activeSockets = new HashMap<WebSocket,SEPAResponseListener>();
		
	public class SEPAResponseListener implements ResponseAndNotificationListener {
		private WebSocket socket;	
		private HashSet<String> spuIds = new HashSet<String>();
		
		public void unsubscribeAll() {
			synchronized(spuIds) {
				Iterator<String> it = spuIds.iterator();
				
				while(it.hasNext()) {
					Integer token = scheduler.getToken();
					SEPALogger.log(VERBOSITY.DEBUG, tag, ">> Scheduling UNSUBSCRIBE request #"+token);
					scheduler.addRequest(new UnsubscribeRequest(token,it.next()),this);		
				}
			}
		}
		
		@Override
		public void notify(Response response) {		
			if (response.getClass().equals(SubscribeResponse.class)) {
				SEPALogger.log(VERBOSITY.DEBUG, tag, "<< SUBSCRIBE response #"+response.getToken());
				
				synchronized(spuIds) {
					spuIds.add(((SubscribeResponse)response).getSPUID());
				}
			
			}else if(response.getClass().equals(UnsubscribeResponse.class)) {
				SEPALogger.log(VERBOSITY.DEBUG, tag, "<< UNSUBSCRIBE response #"+response.getToken()+" ");
				
				synchronized(spuIds) {
					spuIds.remove(((UnsubscribeResponse)response).getSPUID());
				
					synchronized(activeSockets) {
						if (spuIds.isEmpty()) activeSockets.remove(socket);
					}
				}
			}
			
			//Send response to client
			if (socket != null) if (socket.isConnected()) socket.send(response.toString());	
			
			//Release token
			if (!response.getClass().equals(Notification.class)) scheduler.releaseToken(response.getToken());
		}
		
		public Set<String> getSPUIDs() {
			return spuIds;
		}
		
		public SEPAResponseListener(WebSocket socket) {
			this.socket = socket;
		}
	}
	
	public WebSocketGate(Properties properties,Scheduler scheduler) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
		if (scheduler == null) SEPALogger.log(VERBOSITY.ERROR, tag, "Scheduler is null");
		this.scheduler = scheduler;
		
		if (properties == null) SEPALogger.log(VERBOSITY.ERROR, tag, "Properties are null");
		else {
			wsPort = Integer.parseInt(properties.getProperty("wsPort", "9000"));
			keepAlivePeriod =  Integer.parseInt(properties.getProperty("keepAlivePeriod", "5000"));
		}
		
		//Get the MBean server
	    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	    
	    //register the MBean
	    ObjectName name = new ObjectName("arces.unibo.SEPA.server:type=WebSocketGate");
	    mbs.registerMBean(this, name);
		
	}
	
	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		SEPALogger.log(VERBOSITY.DEBUG, tag, "onClose: "+socket.toString());
		
		if (keepAlivePeriod == 0) activeSockets.get(socket).unsubscribeAll();//unsubscribeAllSPUs(socket);
	}

	@Override
	public void onConnect(WebSocket socket) {
		SEPALogger.log(VERBOSITY.DEBUG, tag, "onConnect: "+socket.toString());
		SEPAResponseListener listener = new SEPAResponseListener(socket);
		
		synchronized(activeSockets) {
			activeSockets.put(socket, listener);
		}
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		Integer token = scheduler.getToken();
		
		Request request = parseRequest(token,text);
		
		if(request == null) {
			SEPALogger.log(VERBOSITY.DEBUG, tag, "Not supported request: "+text);
			
			ErrorResponse response = new ErrorResponse(token,"Not supported request: "+text);
			
			socket.send(response.toString());
			
			scheduler.releaseToken(token);
			
			return;
		}
		
		synchronized(activeSockets) {
			SEPALogger.log(VERBOSITY.DEBUG, tag, ">> Scheduling request: "+request.toString());
			scheduler.addRequest(request,activeSockets.get(socket));	
		}
	}
	
	//TODO SPARQL 1.1 Subscribe language
	private Request parseRequest(Integer token,String request) {
		if (request.trim().startsWith("subscribe=")) return new SubscribeRequest(token,request.substring(request.trim().indexOf("=")+1));
		if (request.trim().startsWith("unsubscribe=")) return new UnsubscribeRequest(token,request.substring(request.trim().indexOf("=")+1));
		return null;
	}
	
	public int getPort() {
		return wsPort;
	}
	
	public boolean start(){
		final HttpServer server = HttpServer.createSimpleServer("/var/www", wsPort);

        // Register the WebSockets add on with the HttpServer
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());

        // register the application
        WebSocketEngine.getEngine().register("", "/sparql", this);

        try {
			server.start();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.INFO, tag, "Failed to start WebSocket gate on port "+wsPort+ " "+e.getMessage());
			return false;
		}
		
		SEPALogger.log(VERBOSITY.INFO, tag, "Started on port "+wsPort);

		if (keepAlivePeriod > 0) {
			new KeepAlive().start();
		}
		return true;
	}

	
	/*private void unsubscribeAllSPUs(WebSocket socket) {
		synchronized(activeSockets) {
			SEPAResponseListener listener = activeSockets.get(socket);
			Iterator<String> it = listener.getSPUIDs().iterator();
			
			while(it.hasNext()) {
				Integer token = scheduler.getToken();
				SEPALogger.log(VERBOSITY.DEBUG, tag, ">> Scheduling UNSUBSCRIBE request #"+token);
				scheduler.addRequest(new UnsubscribeRequest(token,it.next()),listener);		
			}
		}
	}*/
	
	public class KeepAlive extends Thread {//implements ResponseListener{
		public void run() {
			while(true) {
				try {
					Thread.sleep(keepAlivePeriod);
				} catch (InterruptedException e) {
					return;
				}
				
				//Send heart beat on each active socket to detect broken sockets
				//HashSet<WebSocket> brokenSockets = new HashSet<WebSocket>();
				
				synchronized(activeSockets) {
					for(WebSocket socket : activeSockets.keySet()) {	
						
						if (socket.isConnected()) {
							Ping ping = new Ping();
							socket.send(ping.toString());
						}
						else {
							activeSockets.get(socket).unsubscribeAll();
						}//brokenSockets.add(socket);
					}
				}
				
				/*
				//Send a UNSUBSCRIBE request to all SPUs belonging to broken sockets
				for (WebSocket socket : brokenSockets) {
					unsubscribeAllSPUs(socket);
				}*/
					
			}
		}
	}

	@Override
	public int getActiveWebSockets() {
		return this.activeSockets.size();
	}

}
