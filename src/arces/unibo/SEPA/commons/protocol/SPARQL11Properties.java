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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SPARQL11Properties {
	private static final Logger logger = LogManager.getLogger("SPARQL11ProtocolProperties");
	protected String header = "---SPARQL 1.1 Service properties file ---";
	
	public enum SPARQLPrimitive {QUERY,UPDATE};
	public enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	public enum QueryResultsFormat {JSON,XML,CSV};
	public enum UpdateResultsFormat {HTML,JSON};
	
	//Properties file
	protected String defaultsFileName = "endpoint.defaults";
	protected String propertiesFile = "endpoint.properties";
	protected Properties properties = new Properties();
	private boolean loaded = false;
	
	public SPARQL11Properties(String propertiesFile) {
		this.propertiesFile = propertiesFile;
		
		loaded = loadProperties();
	}
	
	public boolean loaded() {
		return loaded;
	}
	
	protected void defaults() {
		properties.setProperty("host", "localhost");
		properties.setProperty("httpPort", "8000");
		properties.setProperty("queryPath", "/sparql");
		properties.setProperty("updatePath", "/sparql");
		properties.setProperty("queryMethod", "POST");
		properties.setProperty("updateMethod", "POST");
		properties.setProperty("queryResultsFormat", "JSON");
		properties.setProperty("updateResultsFormat", "HTML");
		properties.setProperty("httpScheme", "http");
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
	
	protected boolean storeProperties(String propertiesFile) {		
		FileOutputStream out;
		try {
			out = new FileOutputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Error on opening properties file: "+propertiesFile);
			return false;
		}
		try {
			properties.store(out, header);
		} catch (IOException e) {
			logger.error("Error on storing properties file: "+propertiesFile);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			logger.error("Error on closing properties file: "+propertiesFile);
			return false;
		}
		return true;
	}
	
	public String getHost() {
		return properties.getProperty("host", "localhost");
	}
	
	public String getHttpScheme() {
		return properties.getProperty("httpScheme", "http");
	}
	
	public int getHttpPort() {
		return Integer.decode(properties.getProperty("httpPort", "9999"));
	}
	
	public String getQueryPath() {
		return properties.getProperty("queryPath", "/blazegraph/namespace/kb/sparql");
	}
	
	public String getUpdatePath() {
		return properties.getProperty("updatePath",  "/blazegraph/namespace/kb/sparql");
	}
	
	public HTTPMethod getQueryMethod() {
		switch(properties.getProperty("queryMethod", "POST").toUpperCase()){
			case "POST": return HTTPMethod.POST;
			case "GET": return HTTPMethod.GET;
			case "URL_ENCODED_POST": return HTTPMethod.URL_ENCODED_POST;
			default: return HTTPMethod.POST;
		}	
	}
	
	public QueryResultsFormat getQueryResultsFormat() {
		if(properties.getProperty("queryResultsFormat", "JSON").equals("JSON")) return QueryResultsFormat.JSON;
		else if(properties.getProperty("queryResultsFormat", "JSON").equals("XML")) return QueryResultsFormat.XML;
		else return QueryResultsFormat.CSV;
	}
	
	public HTTPMethod getUpdateMethod() {
		if(properties.getProperty("updateMethod", "POST").equals("POST")) return HTTPMethod.POST;
		return HTTPMethod.URL_ENCODED_POST;	
	}
	
/*				
				if(getProperty("updateResultsFormat", "HTML").equals("HTML")) updateResultsFormat = UpdateResultsFormat.HTML;
				else updateResultsFormat = UpdateResultsFormat.JSON;

	*/
}
