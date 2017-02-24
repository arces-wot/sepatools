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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import arces.unibo.SEPA.application.SEPALogger;
import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;

/**
 * This class represents the SPARQL Subscription (SUB) Engine of the Semantic Event Processing Architecture (SEPA)
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class Engine extends Thread {
	//Properties and logging
	private static String tag ="SUBEngine";
	private final String defaultPropertiesFile = "defaults.properties";
	private final String propertiesFile = "engine.properties";
	private Properties properties = new Properties();
	
	//Primitives scheduler/dispatcher
	private Scheduler scheduler = null;
	
	//Primitives processor
	private Processor processor = null;
	
	//SPARQL 1.1 Protocol handler
	private HTTPGate httpGate = null;

	//SPARQL 1.1 SE Protocol handler
	private WebSocketGate websocketGate = null;
	
	public Engine() {}
	
	public static void main(String[] args) {
		System.out.println("##########################################################################################");
		System.out.println("# SEPA Engine Ver 0.1  Copyright (C) 2016-2017                                           #");
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
			
		//SEPALogger.loadSettings();
		
		Engine engine = new Engine();
		
		if (engine.init()) {
			SEPALogger.log(VERBOSITY.INFO, tag, "SUB Engine initialized and ready to start");	
		}
		else {
			SEPALogger.log(VERBOSITY.INFO, tag, "Failed to initialize the SUB Engine...exit...");
			return;
		}
		
		//Starting main engine thread
		engine.start();
	}
	
	@Override
	public void start() {
		this.setName("SEPA Engine");
		SEPALogger.log(VERBOSITY.INFO, tag, "SUB Engine starting...");	
		
		httpGate.start();
		websocketGate.start();
		scheduler.start();
		
		super.start();
		SEPALogger.log(VERBOSITY.INFO, tag, "SUB Engine started");	
	}
	
	public boolean init() {
		boolean defaultProperties = false;
		if (!loadProperties(propertiesFile)) {
			if (!loadProperties(defaultPropertiesFile)) defaultProperties = true;
		}
		
		processor = new Processor(properties);
		scheduler = new Scheduler(properties,processor);
		
		httpGate = new HTTPGate(properties,scheduler);
		websocketGate = new WebSocketGate(properties,scheduler);
		
		storeProperties(defaultProperties);
		
		return true;
	}
	
	private boolean loadProperties(String fname){
		FileInputStream in;
		try {
			in = new FileInputStream(fname);
		} catch (FileNotFoundException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+fname);
			return false;
		}
		try {
			properties.load(in);
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on loading properties file: "+fname);
			return false;
		}
		try {
			in.close();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+fname);
			return false;
		}
		
		return true;	
	}
	
	private boolean storeProperties(boolean def) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(defaultPropertiesFile);
		} catch (FileNotFoundException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+defaultPropertiesFile);
			return false;
		}
		try {
			if (def) properties.store(out, "---SUB Engine DEFAULT properties file ---");
			else properties.store(out, "---SUB Engine properties file ---");
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on storing properties file: "+defaultPropertiesFile);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+defaultPropertiesFile);
			return false;
		}
		
		return true;

	}
}
