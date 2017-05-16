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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.logging.log4j.LogManager;

public class EngineProperties {
	private static final Logger logger = LogManager.getLogger("EngineProperties");
	
	private String defaultsFileName = "enginedefaults.json";
	private String propertiesFile = "engine.json";
		
	private JsonObject properties = new JsonObject();
	private boolean loaded;
	
	public EngineProperties(String propertiesFile) {
		this.propertiesFile = propertiesFile;
		
		loaded = loadProperties();
	}
	
	public boolean loaded() {
		return loaded;
	}
	
	protected void defaults() {
		JsonObject port = new JsonObject();
		port.add("ws", new JsonPrimitive(9000));
		port.add("wss", new JsonPrimitive(9443));
		port.add("http", new JsonPrimitive(8000));
		port.add("https", new JsonPrimitive(8443));
		properties.add("ports", port);
		
		JsonObject timeouts = new JsonObject();
		timeouts.add("token", new JsonPrimitive(0));
		timeouts.add("maxtokens", new JsonPrimitive(1000));
		timeouts.add("keepalive", new JsonPrimitive(5000));
		timeouts.add("http", new JsonPrimitive(5000));
		properties.add("timeouts", timeouts);
		
		JsonObject path = new JsonObject();
		path.add("http",new JsonPrimitive("/sparql"));
		path.add("https",new JsonPrimitive("/sparql"));
		path.add("ws",new JsonPrimitive("/sparql"));
		path.add("wss",new JsonPrimitive("/secure/sparql"));
		path.add("register",new JsonPrimitive("/oauth/register"));
		path.add("token",new JsonPrimitive("/oauth/token"));
		properties.add("paths",path);
		
		//Add new properties here...
	}
		
	private boolean loadProperties(){
		FileReader in = null;
		try {
			in = new FileReader(propertiesFile);
			if (in != null) {
				properties = new JsonParser().parse(in).getAsJsonObject();
			}
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
		FileWriter out;
		try {
			out = new FileWriter(propertiesFile);
			out.write(properties.toString());
			out.close();
		} catch (IOException e) {
			logger.error("Write properties file FAILED "+propertiesFile);
			return false;
		}

		return true;
	}
	
	public int getHttpTimeout() {return properties.get("timeouts").getAsJsonObject().get("http").getAsInt();}
	
	public int getWsPort() {return properties.get("ports").getAsJsonObject().get("ws").getAsInt();}

	public int getHttpsPort() {return properties.get("ports").getAsJsonObject().get("https").getAsInt();}

	public int getWssPort() {return properties.get("ports").getAsJsonObject().get("wss").getAsInt();}

	public int getHttpPort() {return properties.get("ports").getAsJsonObject().get("http").getAsInt();}

	public long getTokenTimeout() {return properties.get("timeouts").getAsJsonObject().get("token").getAsInt();}

	public int getMaxTokens() {return properties.get("timeouts").getAsJsonObject().get("maxtokens").getAsInt();}

	public int getKeepAlivePeriod() {return properties.get("timeouts").getAsJsonObject().get("keepalive").getAsInt();}

	public String getHttpPath() {return properties.get("paths").getAsJsonObject().get("http").getAsString();}
	
	public String getHttpsPath() {return properties.get("paths").getAsJsonObject().get("https").getAsString();}
	
	public String getWsPath() {return properties.get("paths").getAsJsonObject().get("ws").getAsString();}
	
	public String getWssPath() {return properties.get("paths").getAsJsonObject().get("wss").getAsString();}

	public String getRegisterPath() {return properties.get("paths").getAsJsonObject().get("register").getAsString();}
	
	public String getTokenRequestPath() {return properties.get("paths").getAsJsonObject().get("token").getAsString();}
}
