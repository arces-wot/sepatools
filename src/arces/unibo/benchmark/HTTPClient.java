package arces.unibo.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;
import arces.unibo.SUBEngine.QueryRequest;
import arces.unibo.SUBEngine.QueryResponse;
import arces.unibo.SUBEngine.SPARQLProtocolClient;
import arces.unibo.SUBEngine.UpdateRequest;
import arces.unibo.SUBEngine.UpdateResponse;

public class HTTPClient {
	private static String tag = "SPARQL Client";
	
	private static Properties properties = new Properties();
	
	private static boolean loadProperties(String fname){
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
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Logger.registerTag("SPARQL Client");
		Logger.enableConsoleLog();
		Logger.setVerbosityLevel(VERBOSITY.DEBUG);
	
		loadProperties("engine.properties");
		SPARQLProtocolClient client = new SPARQLProtocolClient(properties);
		
		String query = "prefix bd:<http://www.bigdata.com/rdf#> "
				+ "prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
				+ "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "select ?x where {?x rdf:type rdfs:Class}";
		String update = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "prefix bd: <http://www.bigdata.com/rdf#> "
				+ "insert data {bd:C4 rdf:type rdfs:Class}";
		
		UpdateResponse updateResponse;
		QueryResponse queryResponse;
		
		updateResponse=client.update(new UpdateRequest(1,update));
		queryResponse=client.query(new QueryRequest(1,query));
		
		Logger.log(VERBOSITY.INFO, tag, updateResponse.getString());
		Logger.log(VERBOSITY.INFO, tag, queryResponse.getString());
	}
}