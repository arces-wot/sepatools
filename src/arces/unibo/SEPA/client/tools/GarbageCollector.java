package arces.unibo.SEPA.client.tools;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.client.pattern.Aggregator;
import arces.unibo.SEPA.client.pattern.ApplicationProfile;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;

public class GarbageCollector extends Aggregator {
	private int processedMessages = 0;
	
	private static final Logger logger = LogManager.getLogger("GarbageCollector");
	
	private static GarbageCollector chatServer;
	
	public GarbageCollector(ApplicationProfile appProfile, String subscribeID, String updateID) {
		super(appProfile,subscribeID, updateID);
	}

	@Override
	public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribe(BindingsResults bindingsResults, String spuid) {
		for (Bindings bindings : bindingsResults.getBindings()) {
			processedMessages++;
			logger.info( processedMessages+ " "+bindings.toString());
			update(bindings);
		}	
	}
	
	public static void main(String[] args) {
		
		ApplicationProfile profile = new ApplicationProfile();
		if(!profile.load("GarbageCollector.sap")) return;
		
		chatServer = new GarbageCollector(profile,"GARBAGE","REMOVE");
		
		if (!chatServer.join()) return;
		if (chatServer.subscribe(null) == null) return;
		
		logger.info("Up and running");
		logger.info("Press any key to exit...");
		
		try {
			System.in.read();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public void brokenSubscription() {
		// TODO Auto-generated method stub
		
	}

}
