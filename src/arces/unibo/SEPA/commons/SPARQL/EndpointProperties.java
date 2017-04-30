package arces.unibo.SEPA.commons.SPARQL;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.commons.SPARQL.ProtocolProperties.HTTPMethod;
import arces.unibo.SEPA.commons.SPARQL.ProtocolProperties.ResultsFormat;

public class EndpointProperties extends Properties {
	private static final long serialVersionUID = -1298120618228920084L;
	
	private static final Logger logger = LogManager.getLogger("SPARQL11ProtocolProperties");
			
	
	
	public EndpointProperties(String propertiesFile) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Properties file: "+propertiesFile + " not found. Creating defaults.");
			defaults();
			storeProperties(propertiesFile);
			return;
		}
		try {
			if (in != null) load(in);
		} catch (IOException e) {
			logger.error("Error loading properties file: "+propertiesFile);
			defaults();
			storeProperties(propertiesFile);
			return;
		}
		try {
			if (in != null) in.close();
		} catch (IOException e) {
			logger.error("Error closing properties file: "+propertiesFile);
		}
	}
	
	protected void defaults() {
		setProperty("host", "localhost");
		setProperty("httpPort", "8000");
		setProperty("queryPath", "/sparql");
		setProperty("updatePath", "/sparql");
		setProperty("queryMethod", "POST");
		setProperty("updateMethod", "POST");
		setProperty("queryResultsFormat", "JSON");
		setProperty("httpScheme", "http");
	}
	
	protected void storeProperties(String propertiesFile) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Error on opening properties file: "+propertiesFile);
			return ;
		}
		try {
			store(out, "---SPARQL 1.1 Service properties file ---");
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
	
	public String getHost() {
		return getProperty("host", "localhost");
	}
	
	public String getHttpScheme() {
		return getProperty("httpScheme", "http");
	}
	
	public int getHttpPort() {
		return Integer.decode(getProperty("httpPort", "9999"));
	}
	
	public String getQueryPath() {
		return getProperty("queryPath", "/blazegraph/namespace/kb/sparql");
	}
	
	public String getUpdatePath() {
		return getProperty("updatePath", "/blazegraph/namespace/kb/sparql");
	}
	
	public HTTPMethod getQueryMethod() {
		if(getProperty("queryMethod", "POST").equals("POST")) return HTTPMethod.POST;
		else if (getProperty("QueryMethod", "POST").equals("GET")) return HTTPMethod.GET;
		else return HTTPMethod.URL_ENCODED_POST;
	}
	
	public ResultsFormat getQueryResultsFormat() {
		if(getProperty("queryResultsFormat", "JSON").equals("JSON")) return ResultsFormat.JSON;
		else if (getProperty("QueryResultsFormat", "JSON").equals("XML")) return ResultsFormat.XML;
		else return ResultsFormat.CSV;
	}
	
	public HTTPMethod getUpdateMethod() {
		if(getProperty("updateMethod", "POST").equals("POST")) return HTTPMethod.POST;
		else if (getProperty("UpdateMethod", "POST").equals("GET")) return HTTPMethod.GET;
		else return HTTPMethod.URL_ENCODED_POST;	
	}
}
