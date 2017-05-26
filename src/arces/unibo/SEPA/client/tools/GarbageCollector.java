package arces.unibo.SEPA.client.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.client.pattern.Aggregator;
import arces.unibo.SEPA.client.pattern.ApplicationProfile;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.response.ErrorResponse;

public class GarbageCollector extends Aggregator {
	private int processedMessages = 0;
	
	private static final Logger logger = LogManager.getLogger("GarbageCollector");
	
	private static GarbageCollector chatServer;
	
	public GarbageCollector(ApplicationProfile appProfile, String subscribeID, String updateID) {
		super(appProfile,subscribeID, updateID);
	}

	@Override
	public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
		
		
	}

	@Override
	public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
		for (Bindings bindings : bindingsResults.getBindings()) {
			processedMessages++;
			logger.info(processedMessages+ " "+bindings.toString());
			update(bindings);
		}
		
	}

	@Override
	public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {
		
		
	}

	@Override
	public void onSubscribe(BindingsResults bindingsResults, String spuid) {
		for (Bindings bindings : bindingsResults.getBindings()) {
			processedMessages++;
			logger.info( processedMessages+ " "+bindings.toString());
			update(bindings);
		}	
	}
	
	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException, DeploymentException, URISyntaxException {
		
		ApplicationProfile profile = new ApplicationProfile("GarbageCollector.jsap");
		
		chatServer = new GarbageCollector(profile,"GARBAGE","REMOVE");
		
		if (chatServer.subscribe(null) == null) return;
		
		logger.info("Up and running");
		logger.info("Press any key to exit...");
		
		try {
			System.in.read();
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}

	@Override
	public void brokenSubscription() {
		
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		
		
	}

}
