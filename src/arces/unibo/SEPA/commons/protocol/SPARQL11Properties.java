/*  This class describes the properties used to access a SPARQL 1.1 Protocol Service
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

package arces.unibo.SEPA.commons.protocol;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class SPARQL11Properties {
	private static final Logger logger = LogManager.getLogger("SPARQL11ProtocolProperties");
	
	public enum SPARQLPrimitive {QUERY,UPDATE};
	public enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	public enum QueryResultsFormat {JSON,XML,CSV};
	public enum UpdateResultsFormat {HTML,JSON};
	
	//Properties file
	protected String defaultsFileName = "endpointdefaults.json";
	protected String propertiesFile = "endpoint.json";
	protected JsonObject properties = new JsonObject();
	private boolean loaded = false;
	
	public SPARQL11Properties(String propertiesFile) {
		this.propertiesFile = propertiesFile;
		
		loaded = loadProperties();
	}
	
	public boolean loaded() {
		return loaded;
	}
	
	protected void defaults() {
		properties.add("host", new JsonPrimitive("localhost"));
		properties.add("port", new JsonPrimitive(9999));
		properties.add("scheme", new JsonPrimitive("http"));
		
		JsonObject query = new JsonObject();
		query.add("path", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		query.add("method", new JsonPrimitive("POST"));
		query.add("format", new JsonPrimitive("JSON"));
		properties.add("query", query);
		
		JsonObject update = new JsonObject();
		update.add("path", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		update.add("method", new JsonPrimitive("URL_ENCODED_POST"));
		update.add("format", new JsonPrimitive("HTML"));
		properties.add("update", update);
	}
	
	protected boolean loadProperties(){
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
	
	protected boolean storeProperties(String propertiesFile) {
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
	
	public String getHost() {
		return properties.get("host").getAsString();
	}
	
	public String getHttpScheme() {
		return properties.get("scheme").getAsString();
	}
	
	public int getHttpPort() {
		return properties.get("port").getAsInt();
	}
	
	public String getQueryPath() {
		return properties.get("query").getAsJsonObject().get("path").getAsString();
	}
	
	public String getUpdatePath() {
		return properties.get("update").getAsJsonObject().get("path").getAsString();
	}
	
	public HTTPMethod getQueryMethod() {
		switch(properties.get("query").getAsJsonObject().get("method").getAsString()){
			case "POST": return HTTPMethod.POST;
			case "GET": return HTTPMethod.GET;
			case "URL_ENCODED_POST": return HTTPMethod.URL_ENCODED_POST;
			default: return HTTPMethod.POST;
		}	
	}
	
	public QueryResultsFormat getQueryResultsFormat() {
		switch(properties.get("query").getAsJsonObject().get("format").getAsString()) {
			case "JSON" : return QueryResultsFormat.JSON;
			case "XML" : return QueryResultsFormat.XML;
			case "CSV": return QueryResultsFormat.CSV;
			default: return QueryResultsFormat.JSON;
		}
	}
	
	public HTTPMethod getUpdateMethod() {
		switch(properties.get("update").getAsJsonObject().get("method").getAsString()){
			case "POST": return HTTPMethod.POST;
			case "URL_ENCODED_POST": return HTTPMethod.URL_ENCODED_POST;
			default: return HTTPMethod.POST;
		}	
	}
	
	public UpdateResultsFormat getUpdateResultsFormat() {
		switch(properties.get("update").getAsJsonObject().get("format").getAsString()) {
			case "JSON" : return UpdateResultsFormat.JSON;
			case "HTML" : return UpdateResultsFormat.HTML;
			default: return UpdateResultsFormat.JSON;
		}
	}
}
