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

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;

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
	
	//Update and subscribe request scheduler
	private Scheduler scheduler = null;
	
	//SPARQL 1.1 Protocol handler
	private HTTPGate httpGate = null;

	//SPARQL 1.1 SE Protocol handler
	private WebSocketGate websocketGate = null;
	
	//Used to assign a unique token to every request
	private TokenHandler tokenHandler = null;
	
	//Underpinning SPARQL 1.1 processing service 
	private Endpoint endpoint = null;
	
	//Requests queue manager
	private RequestResponseHandler requestHandler = null;
	
	public Engine() {}
	
	public static void main(String[] args) {
		System.out.println("SEPA Engine  Copyright (C) 2016-2017  Luca Roffia");
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY");
		System.out.println("This is free software, and you are welcome to redistribute it under certain conditions");
		System.out.println("GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007");
	    		
		Logger.loadSettings();
		
		Engine engine = new Engine();
		
		if (engine.init()) {
			Logger.log(VERBOSITY.INFO, tag, "SUB Engine initialized and ready to start");	
		}
		else {
			Logger.log(VERBOSITY.INFO, tag, "Failed to initialize the SUB Engine...exit...");
			return;
		}
		
		//Starting main engine thread
		engine.start();
	}
	
	@Override
	public void start() {
		this.setName("SEPA Engine");
		Logger.log(VERBOSITY.INFO, tag, "SUB Engine starting...");	
		
		httpGate.start();
		
		scheduler.start();
		
		websocketGate.start();
		
		super.start();
		Logger.log(VERBOSITY.INFO, tag, "SUB Engine started");	
	}
	
	public boolean init() {
		boolean defaultProperties = false;
		if (!loadProperties(propertiesFile)) {
			if (!loadProperties(defaultPropertiesFile)) defaultProperties = true;
		}
		
		endpoint = new Endpoint(properties);
		tokenHandler = new TokenHandler(properties);
		requestHandler = new RequestResponseHandler(properties);
		
		httpGate = new HTTPGate(properties,tokenHandler,requestHandler);
		websocketGate = new WebSocketGate(properties,tokenHandler,requestHandler);
		
		scheduler = new Scheduler(properties,requestHandler,endpoint);
		
		storeProperties(defaultProperties);
		
		return true;
	}
	
	private boolean loadProperties(String fname){
		FileInputStream in;
		try {
			in = new FileInputStream(fname);
		} catch (FileNotFoundException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+fname);
			return false;
		}
		try {
			properties.load(in);
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on loading properties file: "+fname);
			return false;
		}
		try {
			in.close();
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+fname);
			return false;
		}
		
		return true;	
	}
	
	private boolean storeProperties(boolean def) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(defaultPropertiesFile);
		} catch (FileNotFoundException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on opening properties file: "+defaultPropertiesFile);
			return false;
		}
		try {
			if (def) properties.store(out, "---SUB Engine DEFAULT properties file ---");
			else properties.store(out, "---SUB Engine properties file ---");
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on storing properties file: "+defaultPropertiesFile);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, tag, "Error on closing properties file: "+defaultPropertiesFile);
			return false;
		}
		
		return true;

	}
}
