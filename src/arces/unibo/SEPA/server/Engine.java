/* This class is the main entry point of the Semantic Event Processing Architecture (SEPA) Engine
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

import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import arces.unibo.SEPA.beans.EngineMBean;
import arces.unibo.SEPA.beans.SEPABeans;
import arces.unibo.SEPA.security.HTTPSGate;
import arces.unibo.SEPA.security.WSSGate;

/**
 * This class represents the SPARQL Subscription (SUB) Engine of the Semantic Event Processing Architecture (SEPA)
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Engine extends Thread implements EngineMBean {

	//Properties and logging
	private EngineProperties engineProperties = new EngineProperties("engine.properties");
	private SPARQLEndpointProperties endpointProperties = new SPARQLEndpointProperties("endpoint.properties");
	private final Date startDate = new Date(); 
	private static final Logger logger = LogManager.getLogger("Engine");
	protected static String mBeanName = "arces.unibo.SEPA.server:type=Engine";
	
	//Primitives scheduler/dispatcher
	private Scheduler scheduler = null;
	
	//Primitives processor
	private Processor processor = null;
	
	//SPARQL 1.1 Protocol handler
	private HTTPGate httpGate = null;
	private HTTPSGate httpsGate = null;
	
	//SPARQL 1.1 SE Protocol handler
	private WSGate websocketGate = null;
	private WSSGate wssGate = null;
	
	public static void main(String[] args) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		System.out.println("##########################################################################################");
		System.out.println("# SEPA Engine Ver 0.3  Copyright (C) 2016-2017                                           #");
		System.out.println("# University of Bologna (Italy)                                                          #");
		System.out.println("# Contact: luca.roffia@unibo.it                                                          #");
		System.out.println("# This program comes with ABSOLUTELY NO WARRANTY                                         #");                                    
		System.out.println("# This is free software, and you are welcome to redistribute it under certain conditions #");
		System.out.println("# GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007                                    #");
		System.out.println("##########################################################################################");
		System.out.println("Referenced library         						License");                                                                 
		System.out.println("commons-io         				http://www.apache.org/licenses/LICENSE-2.0.html");                         
		System.out.println("commons-logging    				http://www.apache.org/licenses/LICENSE-2.0.html");   
		System.out.println("httpclient         				http://www.apache.org/licenses/LICENSE-2.0.html");  		
		System.out.println("httpcore           				http://www.apache.org/licenses/LICENSE-2.0.html"); 
		System.out.println("log4j              				http://www.apache.org/licenses/LICENSE-2.0.html");  			
		System.out.println("grizzply-websockets-server			https://grizzly.java.net/nonav/license.html");
		System.out.println("gson  						http://www.apache.org/licenses/LICENSE-2.0.html");
		System.out.println("jdom						https://github.com/hunterhacker/jdom/blob/master/LICENSE.txt");
		System.out.println("tyrus-standalone-client 			https://tyrus.java.net/license.html");	
		System.out.println("org.eclipse.paho.client.mqttv3			https://projects.eclipse.org/content/eclipse-public-license-1.0");
		System.out.println("bcprov.jdk15on					https://opensource.org/licenses/MIT");   
		System.out.println("bcpkix-jdk15on 					https://opensource.org/licenses/MIT");   
		
		Engine engine = new Engine();
		
		SEPABeans.registerMBean(engine,mBeanName);
		
		if (engine.init()) {
			logger.info("SUB Engine initialized and ready to start");	
		}
		else {
			logger.info("Failed to initialize the SUB Engine...exit...");
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
		
		//Protocol handlers
		httpGate.start();
		websocketGate.start();
		
		//Secure protocol handlers
		httpsGate.start();
		wssGate.start();
		
		super.start();
		logger.info("SUB Engine started");	
	}
	
	public boolean init() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {		
		processor = new Processor(endpointProperties);
		scheduler = new Scheduler(engineProperties,processor);
		
		//Not secure protocols
		httpGate = new HTTPGate(engineProperties,scheduler);
		websocketGate = new WSGate(engineProperties,scheduler);
		
		//Secure protocols
		httpsGate = new HTTPSGate(engineProperties,scheduler);
		wssGate = new WSSGate(engineProperties,scheduler);
			
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
}
