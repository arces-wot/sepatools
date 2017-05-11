/* This class implements the configuration properties of the Semantic Event Processing Architecture (SEPA) Engine
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
package arces.unibo.SEPA.scheduling;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

public class EngineProperties {
	private String propertiesFile = "engine.properties";
	private static final Logger logger = LogManager.getLogger("EngineProperties");
	private Properties properties = new Properties();
	
	private int httpTimeout;
	private int wsPort;
	private int httpsPort;
	private int wssPort;
	private int httpPort;
	private long tokenTimeout;
	private int maxTokens;
	private int keepAlivePeriod;
	
	public EngineProperties(String fName) {
		propertiesFile = fName;
		
		//Load from properties file
		loadProperties();
		
		//Store properties on file
		storeProperties();
	}
	
	public EngineProperties() {		
		//Load from properties file
		loadProperties();
		
		//Store properties on file
		storeProperties();
	}
	
	protected void getProperties() {
		httpTimeout = Integer.parseInt(properties.getProperty("httpTimeout", "2000"));
		
		wsPort = Integer.parseInt(properties.getProperty("wsPort", "9000"));
		httpsPort = Integer.parseInt(properties.getProperty("httpsPort", "8443"));
		wssPort = Integer.parseInt(properties.getProperty("wssPort", "9443"));
		httpPort = Integer.parseInt(properties.getProperty("httpPort", "8000"));	
		
		tokenTimeout = Integer.parseInt(properties.getProperty("tokenTimeout", "0"));
		maxTokens = Integer.parseInt(properties.getProperty("maxTokens", "1000"));
		keepAlivePeriod = Integer.parseInt(properties.getProperty("keepAlivePeriod", "5000"));
			
		//Add new properties here...
	}
	
	protected void setProperties() {
		properties.setProperty("httpTimeout",String.format("%d", httpTimeout));
		
		properties.setProperty("wsPort",String.format("%d", wsPort));
		properties.setProperty("httpsPort",String.format("%d", httpsPort));
		properties.setProperty("wssPort",String.format("%d", wssPort));
		properties.setProperty("httpPort",String.format("%d", httpPort));
		
		properties.setProperty("tokenTimeout",String.format("%d", tokenTimeout));
		properties.setProperty("maxTokens",String.format("%d", maxTokens));
		properties.setProperty("keepAlivePeriod",String.format("%d", keepAlivePeriod));
		
		//Add new properties here...
	}
	
	protected void loadProperties(){
		FileInputStream in = null;
		try {
			in = new FileInputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Properties file: "+propertiesFile + " not found");
		}
		try {
			if (in != null) properties.load(in);
		} catch (IOException e) {
			logger.error("Error loading properties file: "+propertiesFile);
		}
		try {
			if (in != null) in.close();
		} catch (IOException e) {
			logger.error("Error closing properties file: "+propertiesFile);

		}
		
		getProperties();
	}
	
	protected void storeProperties() {
		//Set current properties
		setProperties();
		
		FileOutputStream out;
		try {
			out = new FileOutputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Error on opening properties file: "+propertiesFile);
			return ;
		}
		try {
			properties.store(out, "---SUB Engine properties file ---");
		} catch (IOException e) {
			logger.error("Error on storing properties file: "+propertiesFile);
			return ;
		}
		try {
			out.close();
		} catch (IOException e) {
			logger.error("Error on closing properties file: "+propertiesFile);
			return ;
		}
	}
	
	public int getHttpTimeout() {
		return httpTimeout;
	}

	public int getWsPort() {
		return wsPort;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	public int getWssPort() {
		return wssPort;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public long getTokenTimeout() {
		return tokenTimeout;
	}

	public int getMaxTokens() {
		return maxTokens;
	}

	public int getKeepAlivePeriod() {
		return keepAlivePeriod;
	}

	public void setTokenTimeout(long timeout) {
		tokenTimeout = timeout;
	}
}
