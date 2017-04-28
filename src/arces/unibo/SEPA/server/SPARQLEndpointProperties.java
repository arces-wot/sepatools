package arces.unibo.SEPA.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SPARQLEndpointProperties {
	private String propertiesFile = "endpoint.properties";
	private static final Logger logger = LogManager.getLogger("EndpointProperties");
	private Properties properties = new Properties();
	
	private String scheme;
	private String host;
	private int port;
	private String queryPath;
	private String updatePath;
	private HTTPMethod queryMethod;
	private HTTPMethod updateMethod;
	private ResultsFormat resultsFormat;
		
	public enum HTTPMethod {GET,POST,URL_ENCODED_POST};
	public enum ResultsFormat {JSON,XML,CSV};	
	
	public SPARQLEndpointProperties(String fName) {
		propertiesFile = fName;
		
		//Load from properties file
		loadProperties();
		
		//Store proprieties on file
		storeProperties();
	}
	
	protected void getProperties() {
		setHttpScheme(properties.getProperty("endpointHttpScheme", "http"));
		setHost(properties.getProperty("endpointHost", "localhost"));
		setPort(Integer.parseInt(properties.getProperty("endpointPort", "9999")));
		setQueryPath(properties.getProperty("endpointQueryPath", "/blazegraph/namespace/kb/sparql"));
		setUpdatePath(properties.getProperty("endpointUpdatePath", "/blazegraph/namespace/kb/sparql"));
		
		if(properties.getProperty("endpointQueryMethod", "POST").equals("POST"))		setQueryMethod(HTTPMethod.POST);
		else if (properties.getProperty("endpointQueryMethod", "POST").equals("GET")) 	setQueryMethod(HTTPMethod.GET);
		else setQueryMethod(HTTPMethod.URL_ENCODED_POST);
		
		if(properties.getProperty("endpointUpdateMethod", "POST").equals("POST"))	setUpdateMethod(HTTPMethod.POST);
		else if (properties.getProperty("endpointUpdateMethod", "POST").equals("GET")) setUpdateMethod(HTTPMethod.GET);
		else setUpdateMethod(HTTPMethod.URL_ENCODED_POST);
		
		if(properties.getProperty("endpointQueryResultsFormat", "JSON").equals("JSON")) setQueryResultsFormat(ResultsFormat.JSON);
		else if (properties.getProperty("endpointQueryResultsFormat", "JSON").equals("XML")) setQueryResultsFormat(ResultsFormat.XML);
		else setQueryResultsFormat(ResultsFormat.CSV);
		
		//Add new properties here...
	}
	
	protected void setProperties() {
		properties.setProperty("endpointHttpScheme",getHttpScheme());
		properties.setProperty("endpointHost",getHost());
		properties.setProperty("endpointPort",String.format("%d", getPort()));
		properties.setProperty("endpointQueryPath",getQueryPath());
		properties.setProperty("endpointUpdatePath",getUpdatePath());
		
		if(getQueryMethod().equals(HTTPMethod.GET)) properties.setProperty("endpointQueryMethod","GET");
		else if(getQueryMethod().equals(HTTPMethod.POST))properties.setProperty("endpointQueryMethod","POST");
		else properties.setProperty("endpointQueryMethod","URL_ENCODED_POST");
		
		if(getUpdateMethod().equals(HTTPMethod.GET)) properties.setProperty("endpointUpdateMethod","GET");
		else if(getUpdateMethod().equals(HTTPMethod.POST)) properties.setProperty("endpointUpdateMethod","POST");
		else properties.setProperty("endpointUpdateMethod","URL_ENCODED_POST");
		
		if(getQueryResultsFormat().equals(ResultsFormat.JSON)) properties.setProperty("endpointQueryResultsFormat","JSON");
		else if(getQueryResultsFormat().equals(ResultsFormat.XML)) properties.setProperty("endpointQueryResultsFormat","XML");
		else properties.setProperty("endpointQueryResultsFormat","CSV");
		
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
	
	public String getHttpScheme() {
		return scheme;
	}
	public void setHttpScheme(String endpointHttpScheme) {
		this.scheme = endpointHttpScheme;
	}
	
	public String getHost() {
		return host;
	}
	public void setHost(String endpointHost) {
		this.host = endpointHost;
	}
	
	public int getPort() {
		return port;
	}
	public void setPort(int endpointPort) {
		this.port = endpointPort;
	}
	
	public String getQueryPath() {
		return queryPath;
	}
	
	public String getUpdatePath() {
		return updatePath;
	}
	
	public void setQueryPath(String queryPath) {
		this.queryPath = queryPath;
	}
	
	public void setUpdatePath(String updatePath) {
		this.updatePath = updatePath;
	}
	
	public HTTPMethod getQueryMethod() {
		return queryMethod;
	}
	
	public void setQueryMethod(HTTPMethod endpointQueryMethod) {
		this.queryMethod = endpointQueryMethod;
	}
	public ResultsFormat getQueryResultsFormat() {
		return resultsFormat;
	}
	public void setQueryResultsFormat(ResultsFormat endpointQueryResultsFormat) {
		this.resultsFormat = endpointQueryResultsFormat;
	}
	public void setUpdateMethod(HTTPMethod post) {
		this.updateMethod = post;	
	}
	public HTTPMethod getUpdateMethod() {
		return updateMethod;	
	}
}
