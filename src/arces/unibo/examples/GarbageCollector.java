package arces.unibo.examples;

import java.io.IOException;

import arces.unibo.SEPA.application.Aggregator;
import arces.unibo.SEPA.application.ApplicationProfile;
import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;

public class GarbageCollector extends Aggregator {
	private int processedMessages = 0;
	private static String tag ="ChatServer";
	private static GarbageCollector chatServer;
	
	public GarbageCollector(ApplicationProfile appProfile, String subscribeID, String updateID) {
		super(appProfile,subscribeID, updateID);
		Logger.log(VERBOSITY.INFO, tag, "Update URL: "+getUpdateURL());
		Logger.log(VERBOSITY.INFO, tag, "SPARQL 1.1 UDPATE: "+appProfile.update(updateID));
		Logger.log(VERBOSITY.INFO, tag, "Subscribe URL: "+getSubscribeURL());
		Logger.log(VERBOSITY.INFO, tag, "SPARQL 1.1 QUERY: "+appProfile.subscribe(subscribeID));
	}

	@Override
	public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
		for (Bindings bindings : bindingsResults.getBindings()) {
			processedMessages++;
			Logger.log(VERBOSITY.INFO, tag, processedMessages+ " "+bindings.toString());
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
			Logger.log(VERBOSITY.INFO, tag, processedMessages+ " "+bindings.toString());
			update(bindings);
		}	
	}
	
	public static void main(String[] args) {
		Logger.setVerbosityLevel(VERBOSITY.INFO);
		Logger.registerTag(tag);
		Logger.enableConsoleLog();
		
		ApplicationProfile profile = new ApplicationProfile();
		if(!profile.load("SAP files/GarbageCollector.sap")) return;
		
		chatServer = new GarbageCollector(profile,"GARBAGE","REMOVE");
		
		if (!chatServer.join()) return;
		if (chatServer.subscribe(null) == null) return;
		
		Logger.log(VERBOSITY.INFO,tag,"Up and running");
		Logger.log(VERBOSITY.INFO,tag,"Press any key to exit...");
		
		try {
			System.in.read();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
