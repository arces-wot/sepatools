/* This class is the main entry point of the Semantic Event Processing Architecture (SEPA) Engine
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

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
package arces.unibo.SEPA.server.core;

import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import arces.unibo.SEPA.commons.protocol.SPARQL11Properties;
import arces.unibo.SEPA.commons.request.Request;

import arces.unibo.SEPA.server.beans.EngineMBean;
import arces.unibo.SEPA.server.beans.SEPABeans;

import arces.unibo.SEPA.server.processing.Processor;

import arces.unibo.SEPA.server.protocol.HTTPGate;
import arces.unibo.SEPA.server.protocol.HTTPSGate;
import arces.unibo.SEPA.server.protocol.WSGate;
import arces.unibo.SEPA.server.protocol.WSSGate;

import arces.unibo.SEPA.server.scheduling.Scheduler;
import arces.unibo.SEPA.server.scheduling.SchedulerInterface;
import arces.unibo.SEPA.server.scheduling.RequestResponseHandler.ResponseAndNotificationListener;

/**
 * This class represents the SPARQL Subscription (SUB) Engine of the Semantic Event Processing Architecture (SEPA)
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.6
* */

public class Engine extends Thread implements EngineMBean,SchedulerInterface {
	private static final Logger logger = LogManager.getLogger("Engine");
	
	//Properties, logging and JMX
	private EngineProperties engineProperties = new EngineProperties("engine.properties");
	private SPARQL11Properties endpointProperties = new SPARQL11Properties("endpoint.properties");
	
	private final Date startDate = new Date(); 
	
	protected static String mBeanName = "arces.unibo.SEPA.scheduling:type=Engine";
	
	//Primitives scheduler/dispatcher
	private Scheduler scheduler = null;
	
	//Primitives processor
	private Processor processor = null;
	
	//SPARQL 1.1 Protocol handler
	private HTTPGate httpGate = null;
	private HTTPSGate httpsGate = null;
	
	//SPARQL 1.1 SE Protocol handler
	private WSGate websocketApp;
	private WSSGate secureWebsocketApp;
	
	public static void main(String[] args) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		System.out.println("##########################################################################################");
		System.out.println("# SEPA Engine Ver 0.6  Copyright (C) 2016-2017                                           #");
		System.out.println("# University of Bologna (Italy)                                                          #");
		System.out.println("#                                                                                        #");
		System.out.println("# This program comes with ABSOLUTELY NO WARRANTY                                         #");                                    
		System.out.println("# This is free software, and you are welcome to redistribute it under certain conditions #");
		System.out.println("# GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007                                    #");
		System.out.println("#                                                                                        #");
		System.out.println("# GitHub: https://github.com/vaimee/sepatools                                            #");
		System.out.println("# Web: http://wot.arces.unibo.it                                                         #");
		System.out.println("##########################################################################################");
		System.out.println("");		
		System.out.println("Dependencies");
		System.out.println("com.google.code.gson          2.8.0       Apache 2.0");
		System.out.println("com.nimbusds                  4.34.2      The Apache Software License, Version 2.0");
		System.out.println("commons-io                    2.5         Apache License, Version 2.0");
		System.out.println("commons-logging               1.2         The Apache Software License, Version 2.0");
		System.out.println("org.apache.httpcomponents     4.5.3       Apache License, Version 2.0");
		System.out.println("org.apache.httpcomponents     4.4.6       Apache License, Version 2.0");
		System.out.println("org.apache.logging.log4j      2.8.1       Apache License, Version 2.0");
		System.out.println("org.bouncycastle              1.56        Bouncy Castle Licence");
		System.out.println("org.eclipse.paho              1.1.1       Eclipse Public License - Version 1.0");
		System.out.println("org.glassfish.grizzly         2.3.30      CDDL+GPL");
		System.out.println("org.glassfish.tyrus.bundles   1.13.1      Dual license consisting of the CDDL v1.1 and GPL v2");
		System.out.println("org.jdom                      2.0.6       Similar to Apache License but with the acknowledgment clause removed");
		System.out.println("");
		
		Engine engine = new Engine();
		
		SEPABeans.registerMBean(engine,mBeanName);
		
		if (engine.init()) {
			logger.info("SUB Engine initialized and ready to start");	
		}
		else {
			logger.fatal("Failed to initialize the SUB Engine...exit...");
			System.exit(1);
		}
		
		//Starting main engine thread
		engine.start();
	}
	
	@Override
	public void start() {
		this.setName("SEPA Engine");
		logger.info("SUB Engine starting...");	
		
		//Scheduler
		scheduler.start();
		
		//SPARQL 1.1 Protocol handlers
		httpGate.start();
		httpsGate.start();
		
		//SPARQL 1.1 SE Protocol handler for WebSocket based subscriptions
		websocketApp.start();
		secureWebsocketApp.start();
		
		super.start();
		logger.info("SUB Engine started");	
		System.out.println("");	
	}
	
	public boolean init() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {			
		if (!endpointProperties.loaded() || !engineProperties.loaded()) return false;
		
		processor = new Processor(endpointProperties);
		scheduler = new Scheduler(engineProperties,processor);
		
		//SPARQL 1.1 Protocol handlers
		httpGate = new HTTPGate(engineProperties,this);
		httpsGate = new HTTPSGate(engineProperties,this);
		
		//SPARQL 1.1 SE Protocol handler for WebSocket based subscriptions
		websocketApp = new WSGate(engineProperties,scheduler);
		secureWebsocketApp = new WSSGate(engineProperties,scheduler);
		
		return true;
	}
	
	@Override
	public EngineProperties getProperties() {
		return this.engineProperties;
	}

	@Override
	public Date getStartDate() {
		return this.startDate;
	}

	@Override
	public int getToken() {
		if (scheduler != null) return scheduler.getToken();
		return -1;
	}

	@Override
	public void addRequest(Request request, ResponseAndNotificationListener listener) {
		if (scheduler != null) scheduler.addRequest(request, listener);
		
	}

	@Override
	public void releaseToken(Integer token) {
		if (scheduler != null) scheduler.releaseToken(token);
	}
}
