/* This class implements the configuration properties of the Semantic Event Processing Architecture (SEPA) Engine
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

public class EngineProperties {
	private static final Logger logger = LogManager.getLogger("EngineProperties");
	
	private String defaultsFileName = "engine.defaults";
	private String propertiesFile = "engine.properties";
		
	private Properties properties = new Properties();
	private boolean loaded;
	
	public EngineProperties(String propertiesFile) {
		this.propertiesFile = propertiesFile;
		
		loaded = loadProperties();
	}
	
	public boolean loaded() {
		return loaded;
	}
	
	protected void defaults() {
		properties.setProperty("httpTimeout","2000");
		
		properties.setProperty("wsPort","9000");
		properties.setProperty("httpsPort","8443");
		properties.setProperty("wssPort","9443");
		properties.setProperty("httpPort","8000");
		
		properties.setProperty("tokenTimeout","0");
		properties.setProperty("maxTokens","100");
		properties.setProperty("keepAlivePeriod","5000");
		
		properties.setProperty("httpPath","/sparql");
		properties.setProperty("httpsPath","/sparql");
		properties.setProperty("wsPath","/sparql");
		properties.setProperty("wssPath","/sparql");
		
		properties.setProperty("registerPath","/oauth/register");
		properties.setProperty("tokenRequestPath","/oauth/token");
		
		//Add new properties here...
	}
		
	private boolean loadProperties(){
		FileInputStream in = null;
		try {
			in = new FileInputStream(propertiesFile);
			if (in != null) properties.load(in);
			if (in != null) in.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
			
			defaults();
			if(storeProperties(defaultsFileName)) {
				logger.warn("USING DEFAULTS. Edit \""+defaultsFileName+"\" and rename it to \""+propertiesFile+"\"");
			}
			return false;
		}		
		return true;
	}
	
	private boolean storeProperties(String propertiesFile) {		
		FileOutputStream out;
		try {
			out = new FileOutputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Open properties file FAILED "+propertiesFile);
			return false;
		}
		try {
			properties.store(out, "---SUB Engine properties file ---");
		} catch (IOException e) {
			logger.error("Store properties file FAILED "+propertiesFile);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			logger.error("Close properties file FAILED "+propertiesFile);
			return false;
		}
		return true;
	}
	
	public int getHttpTimeout() {return Integer.parseInt(properties.getProperty("httpTimeout", "2000"));}
	
	public int getWsPort() {return Integer.parseInt(properties.getProperty("wsPort", "9000"));}

	public int getHttpsPort() {return Integer.parseInt(properties.getProperty("httpsPort", "8443"));}

	public int getWssPort() {return Integer.parseInt(properties.getProperty("wssPort", "9443"));}

	public int getHttpPort() {return Integer.parseInt(properties.getProperty("httpPort", "8000"));}

	public long getTokenTimeout() {return Integer.parseInt(properties.getProperty("tokenTimeout", "0"));}

	public int getMaxTokens() {return Integer.parseInt(properties.getProperty("maxTokens", "1000"));}

	public int getKeepAlivePeriod() {return Integer.parseInt(properties.getProperty("keepAlivePeriod", "5000"));}

	public String getHttpPath() {return properties.getProperty("httpPath", "/sparql");}
	
	public String getHttpsPath() {return properties.getProperty("httpsPath", "/sparql");}
	
	public String getWsPath() {return properties.getProperty("wsPath", "/sparql");}
	
	public String getWssPath() {return properties.getProperty("wssPath", "/sparql");}

	public String getRegisterPath() {return properties.getProperty("registerPath", "/oauth/register");}
	
	public String getTokenRequestPath() {return properties.getProperty("tokenRequestPath", "/oauth/token");}
}
