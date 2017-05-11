package arces.unibo.SEPA.client.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SPARQL11Properties extends Properties {
	private static final long serialVersionUID = -1298120618228920084L;
	
	private static final Logger logger = LogManager.getLogger("SPARQL11ProtocolProperties");
	
	public enum SPARQLPrimitive {QUERY,UPDATE};
	public enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	public enum QueryResultsFormat {JSON,XML,CSV};
	public enum UpdateResultsFormat {HTML,JSON};
	
	//Properties file
	private String propertiesFile;
	protected String header = "---SPARQL 1.1 Service properties file ---";
	
	//Properties variables
	private String host ="localhost";
	private int httpPort = 8000;
	private String queryPath="/sparql";
	private String updatePath ="/sparql";
	private HTTPMethod queryMethod = HTTPMethod.POST;
	private HTTPMethod updateMethod = HTTPMethod.POST;
	private QueryResultsFormat queryResultsFormat = QueryResultsFormat.JSON;
	private UpdateResultsFormat updateResultsFormat = UpdateResultsFormat.HTML;
	private String httpScheme ="http";
	
	public SPARQL11Properties(String propertiesFile) {
		this.propertiesFile = propertiesFile;
		FileInputStream in = null;
		try {
			in = new FileInputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Properties file: "+propertiesFile + " not found. Creating defaults.");
			defaults();
			storeProperties();
			return;
		}
		try {
			if (in != null) {
				load(in);
				host = getProperty("host", "localhost");
				httpPort = Integer.decode(getProperty("httpPort", "8000"));
				queryPath = getProperty("queryPath", "/sparql");
				updatePath= getProperty("updatePath", "/sparql");
				httpScheme = getProperty("httpScheme", "http");
				
				if(getProperty("queryMethod", "POST").equals("POST")) queryMethod = HTTPMethod.POST;
				else if(getProperty("queryMethod", "POST").equals("GET")) queryMethod = HTTPMethod.GET;
				else queryMethod = HTTPMethod.URL_ENCODED_POST;
				
				if(getProperty("updateMethod", "POST").equals("POST")) updateMethod = HTTPMethod.POST;
				else updateMethod = HTTPMethod.URL_ENCODED_POST;

				if(getProperty("queryResultsFormat", "JSON").equals("JSON")) queryResultsFormat = QueryResultsFormat.JSON;
				else if(getProperty("queryResultsFormat", "JSON").equals("XML")) queryResultsFormat = QueryResultsFormat.XML;
				else queryResultsFormat = QueryResultsFormat.CSV;
				
				if(getProperty("updateResultsFormat", "HTML").equals("HTML")) updateResultsFormat = UpdateResultsFormat.HTML;
				else updateResultsFormat = UpdateResultsFormat.JSON;
			}
		} catch (IOException e) {
			logger.error("Error loading properties file: "+propertiesFile);
			defaults();
			storeProperties();
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
		setProperty("updateResultsFormat", "HTML");
		setProperty("httpScheme", "http");
	}
	
	protected void storeProperties() {
		FileOutputStream out;
		try {
			out = new FileOutputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			logger.error("Error on opening properties file: "+propertiesFile);
			return ;
		}
		try {
			store(out,header);
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
		return host;
	}
	
	public String getHttpScheme() {
		return httpScheme;
	}
	
	public int getHttpPort() {
		return httpPort;
	}
	
	public String getQueryPath() {
		return queryPath;
	}
	
	public String getUpdatePath() {
		return updatePath;
	}
	
	public HTTPMethod getQueryMethod() {
		return queryMethod;
	}
	
	public QueryResultsFormat getQueryResultsFormat() {
		return queryResultsFormat;
	}
	
	public UpdateResultsFormat getUpdateResultsFormat() {
		return updateResultsFormat;
	}
	
	public HTTPMethod getUpdateMethod() {
		return updateMethod;	
	}
}
