package arces.unibo.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import arces.unibo.SEPA.application.ApplicationProfile;
import arces.unibo.SEPA.application.GenericClient;
import arces.unibo.SEPA.application.SEPALogger;
import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.application.Producer;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;

//INSERT_LAMP
public class StressTest {
	static long NUPDATE = 5;
	static long NQUERY = 3;
	static List<UpdateThread> updateThreads = new ArrayList<UpdateThread>();
	static List<QueryThread> queryThreads = new ArrayList<QueryThread>();
	static List<Thread> activeThreads = new ArrayList<Thread>();
	
	static class UpdateThread extends Producer implements Runnable {
		private String tag = "Update";
		public UpdateThread(ApplicationProfile appProfile) {
			super(appProfile, "INSERT_LAMP");
		}

		private boolean running = true;

		public void run() {
			while(running) {
				double rnd = Math.random() * 3000;
				long sleep = Math.round(rnd);
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return ;
				}
				Bindings bindings = new Bindings();
				bindings.addBinding("lamp", new RDFTermURI("bench:Lamp_"+UUID.randomUUID().toString()));
								
				long timing = System.nanoTime();
		    	
				update(bindings);
		    	
				timing = System.nanoTime() - timing;
				
				SEPALogger.log(VERBOSITY.INFO, tag, "Timing(ns) "+timing);
			}
		}
	}
	
	static class QueryThread extends GenericClient implements Runnable {
		public QueryThread(String url, int updatePort, int subscribePort, String path) {
			super(url, updatePort, subscribePort, path);
		}
		
		public QueryThread(ApplicationProfile appProfile) {
			super(appProfile.getParameters().getUrl(), 
					appProfile.getParameters().getUpdatePort(), 
					appProfile.getParameters().getSubscribePort(), 
					appProfile.getParameters().getPath());
		}
			
		private boolean running = true;

		public void run() {
			while(running) {
				double rnd = Math.random() * 3000;
				long sleep = Math.round(rnd);
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return;
				}
		
				query("select * where {?x ?y ?z}",null);
		    
			}
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static void main(String[] args) {
		ApplicationProfile appProfile =  new ApplicationProfile();
		appProfile.load("LightingBenchmark.sap");
		
		SEPALogger.setVerbosityLevel(VERBOSITY.INFO);
		SEPALogger.enableConsoleLog();
		SEPALogger.registerTag("*");
		
		for (int i=0; i < NUPDATE ; i++) {
			updateThreads.add(new UpdateThread(appProfile));
		}
		
		for (int i=0; i < NQUERY ; i++) {
			queryThreads.add(new QueryThread(appProfile));
		}
		
		for (UpdateThread th: updateThreads) {
			Thread run = new Thread(th);
			activeThreads.add(run);
			run.start();
		}
		for (QueryThread th: queryThreads) {
			Thread run = new Thread(th);
			activeThreads.add(run);
			run.start();
		}
		
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(Thread th : activeThreads) while(!th.isInterrupted()) th.interrupt();
		
	}
}
