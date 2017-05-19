package arces.unibo.SEPA.client.tools;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import arces.unibo.SEPA.client.pattern.ApplicationProfile;
import arces.unibo.SEPA.client.pattern.Producer;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.RDFTermLiteral;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;

public class RealTimeIoTResourceUpdate {
	private static int nThreads = 10;
	private static int nUpdates = 1000;
	
	private static ExecutorService producers = Executors.newFixedThreadPool(nThreads);
	
	public class ProducerThread extends Producer implements Runnable {
		
		
		public ProducerThread(ApplicationProfile appProfile, String updateID) {
			super(appProfile, updateID);
		}

		@Override
		public void run() {
			int i = 0;
			Bindings bindings = new Bindings();
			bindings.addBinding("resource", new RDFTermURI("iot:Resource_"+UUID.randomUUID().toString()));
			
			while (i < nUpdates) {
				bindings.addBinding("value", new RDFTermLiteral(String.format("%d", i++)));
				update(bindings);
			}
			
		}
		
	}
	
	public static void main(String[] args) {
		ApplicationProfile app = new ApplicationProfile();
		app.load("sapexamples/GatewayProfile.sap");
		
		for (int i=0; i < nThreads; i++) producers.submit(new RealTimeIoTResourceUpdate().new ProducerThread(app,"UPDATE_RESOURCE"));
		
		try {
			producers.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
