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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
{"parameters":{
	"host":"localhost",
	"port":9999,
	"scheme":"http",
	"path":"/blazegraph/namespace/kb/sparql",
	"query":{"method":"POST","format":"JSON"},
	"update":{"method":"URL_ENCODED_POST","format":"HTML"}}}
*/
public class SPARQL11Properties {
	private static final Logger logger = LogManager.getLogger("SPARQL11ProtocolProperties");
	
	public enum SPARQLPrimitive {QUERY,UPDATE};
	public enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	public enum QueryResultsFormat {JSON,XML,CSV};
	public enum UpdateResultsFormat {HTML,JSON};
	
	//Properties file
	protected String defaultsFileName = "endpoint.defaults";
	protected String propertiesFile = "endpoint.jpar";
	
	protected JsonObject parameters = new JsonObject();
	protected JsonObject doc = new JsonObject();
	
	public SPARQL11Properties(String propertiesFile) throws FileNotFoundException, NoSuchElementException, IOException {
		this.propertiesFile = propertiesFile;
		
		loadProperties();
	}
	
	public String toString() {
		return doc.toString();
	}
	
	protected void defaults() {
		parameters.add("host", new JsonPrimitive("localhost"));
		parameters.add("port", new JsonPrimitive(9999));
		parameters.add("scheme", new JsonPrimitive("http"));
		parameters.add("path", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		
		JsonObject query = new JsonObject();
		query.add("method", new JsonPrimitive("POST"));
		query.add("format", new JsonPrimitive("JSON"));
		parameters.add("query", query);
		
		JsonObject update = new JsonObject();
		update.add("method", new JsonPrimitive("URL_ENCODED_POST"));
		update.add("format", new JsonPrimitive("HTML"));
		parameters.add("update", update);
		
		doc.add("parameters", parameters);
	}
	
	protected void loadProperties() throws FileNotFoundException, NoSuchElementException, IOException
	{
		FileReader in = null;
		try {
			in = new FileReader(propertiesFile);
			if (in != null) {
				doc = new JsonParser().parse(in).getAsJsonObject();
				if (doc.get("parameters") == null) {
					logger.warn("parameters key is missing");
					throw new NoSuchElementException("parameters key is missing");
				}
				parameters = doc.get("parameters").getAsJsonObject();
			}
			if (in != null) in.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
			
			defaults();
			
			storeProperties(defaultsFileName);
				
			logger.warn("USING DEFAULTS. Edit \""+defaultsFileName+"\" and rename it to \""+propertiesFile+"\"");
			
			throw new FileNotFoundException("USING DEFAULTS. Edit \""+defaultsFileName+"\" and rename it to \""+propertiesFile+"\"");
		}		
	}
	
	protected void storeProperties(String propertiesFile) throws IOException{
		FileWriter out = new FileWriter(propertiesFile);
		out.write(doc.toString());
		out.close();
	}
	
	protected int getParameter(String rootKey,String nestedKey,int def) {
		if (rootKey == null) {
			if (parameters.get(nestedKey) != null) return parameters.get(nestedKey).getAsInt();
			return -1;
		}
		if (parameters.get(rootKey) != null) {
			if (parameters.get(rootKey).getAsJsonObject().get(nestedKey) != null)
				return parameters.get(rootKey).getAsJsonObject().get(nestedKey).getAsInt();
			
		}
		if (parameters.get(nestedKey) != null) return parameters.get(nestedKey).getAsInt();
		
		logger.warn(rootKey+" or "+nestedKey+" keys not found");
		return def;
	}
	
	protected String getParameter(String rootKey,String nestedKey,String def) {
		if (rootKey == null) {
			if (parameters.get(nestedKey) != null) return parameters.get(nestedKey).getAsString();
			return "";
		}
		if (parameters.get(rootKey) != null) {
			if (parameters.get(rootKey).getAsJsonObject().get(nestedKey) != null)
				return parameters.get(rootKey).getAsJsonObject().get(nestedKey).getAsString();
			
		}
		if (parameters.get(nestedKey) != null) return parameters.get(nestedKey).getAsString();
		
		logger.warn(rootKey+" or "+nestedKey+" keys not found");
		return def;	
	}
	
	public String getHost() {
		return getParameter(null,"host","localhost");
	}
	
	public String getUpdateScheme() {
		return getParameter("update","scheme","http");
	}
	
	public int getUpdatePort() {
		return getParameter("update","port",9999);
	}
	
	public String getUpdatePath() {
		return getParameter("update","path","/blazegraph/namespace/kb/sparql");
	}
	
	public HTTPMethod getUpdateMethod() {
		switch(getParameter("update","method","URL_ENCODED_POST")){
			case "POST": return HTTPMethod.POST;
			case "URL_ENCODED_POST": return HTTPMethod.URL_ENCODED_POST;
			default: return HTTPMethod.POST;
		}	
	}
	
	public UpdateResultsFormat getUpdateResultsFormat() {
		switch(getParameter("update","format","JSON")) {
			case "JSON" : return UpdateResultsFormat.JSON;
			case "HTML" : return UpdateResultsFormat.HTML;
			default: return UpdateResultsFormat.JSON;
		}
	}
	
	public String getQueryScheme() {
		return getParameter("query","scheme","http");
	}
	
	public int getQueryPort() {
		return getParameter("query","port",9999);
	}
	
	public String getQueryPath() {
		return getParameter("query","path","/blazegraph/namespace/kb/sparql");
	}
	
	public HTTPMethod getQueryMethod() {
		switch(getParameter("query","method","POST")){
			case "POST": return HTTPMethod.POST;
			case "GET": return HTTPMethod.GET;
			case "URL_ENCODED_POST": return HTTPMethod.URL_ENCODED_POST;
			default: return HTTPMethod.POST;
		}	
	}
	
	public QueryResultsFormat getQueryResultsFormat() {
		switch(getParameter("query","format","JSON")) {
			case "JSON" : return QueryResultsFormat.JSON;
			case "XML" : return QueryResultsFormat.XML;
			case "CSV": return QueryResultsFormat.CSV;
			default: return QueryResultsFormat.JSON;
		}
	}
	
	
}
