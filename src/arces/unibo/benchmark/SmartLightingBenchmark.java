package arces.unibo.benchmark;

import java.util.Vector;

import arces.unibo.SEPA.application.Consumer;
import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Producer;
import arces.unibo.SEPA.application.ApplicationProfile;

import arces.unibo.SEPA.commons.ARBindingsResults;
import arces.unibo.SEPA.commons.RDFTermLiteral;
import arces.unibo.SEPA.commons.RDFTermURI;
import arces.unibo.SEPA.commons.BindingsResults;
import arces.unibo.SEPA.commons.Bindings;

import arces.unibo.SEPA.application.Logger.VERBOSITY;

public abstract class SmartLightingBenchmark {
	//Benchmark definition
	private Vector<RoadPool> roadPoll = new Vector<RoadPool>();
	private Vector<Integer> roadSubscriptions = new Vector<Integer>();
	private Vector<Lamp> lampSubscriptions = new Vector<Lamp>();

	private Vector<RoadSubscription> roadSubs = new Vector<RoadSubscription>();
	private Vector<LampSubscription> lampSubs = new Vector<LampSubscription>();
	
	private Producer lampUpdater;
	private Producer roadUpdater;
	protected final String tag = "SmartLightingBenchmark";

	private int lampNotifyN = 0;
	private int roadNotifyN = 0;
	
	public abstract void reset();
	public abstract void runExperiment();
	public abstract void dataset();
	public abstract void subscribe();
	
	//Data set
	protected int firstRoadIndex = 1;
	protected int nRoads = 0;
	
	static ApplicationProfile appProfile = new ApplicationProfile();
	
	private class RoadPool {
		private final int size;
		private final int number;
		private final int firstIndex;
		
		public RoadPool(int number,int size,int firstIndex) {
			this.size = size;
			this.number = number;
			this.firstIndex = firstIndex;
		}

		public int getSize() {
			return size;
		}

		public int getNumber() {
			return number;
		}

		public int getFirstIndex() {
			return firstIndex;
		}
	}
	private class Lamp {
		private final int road;
		private final int post;
		
		public Lamp(int road,int post) {
			this.road = road;
			this.post = post;
		}

		public int getRoad() {
			return road;
		}

		public int getPost() {
			return post;
		}
	}
	
	public void addLampSubscription(int roadIndex,int lampIndex) {
		lampSubscriptions.addElement(new Lamp(roadIndex,lampIndex));
	}
 	public void addRoadSubscription(int roadIndex) {
		roadSubscriptions.addElement(roadIndex);
	}
	public int addRoad(int size,int index) {
		return addRoads(1,size,index);
	}
	public int addRoads(int number,int size,int firstIndex) {
		roadPoll.add(new RoadPool(number,size,firstIndex));
		return firstIndex + number;
	}
	
	public SmartLightingBenchmark() {
		Logger.registerTag(tag);
		Logger.enableConsoleLog();
		Logger.enableFileLog();
		Logger.setVerbosityLevel(VERBOSITY.INFO);
		
		appProfile.load("LightingBenchmark.sap");
		lampUpdater = new Producer(appProfile,"UPDATE_LAMP");
		roadUpdater = new Producer(appProfile,"UPDATE_ROAD");		
	}
	
	private synchronized int incrementLampNotifies() {
		lampNotifyN++;
		return lampNotifyN;
	}
	
	private synchronized int incrementRoadNotifies() {
		roadNotifyN++;	
		return roadNotifyN;
	}
	
	class LampSubscription extends Consumer implements Runnable {	
		private String subID;
		private String lampURI = "";
		private boolean running = true;
		private Bindings bindings = new Bindings();
		private Object sync = new Object();
		
		public LampSubscription(int roadIndex,int lampIndex) {
			super(appProfile,"LAMP");
			lampURI = "bench:Lamp_"+roadIndex+"_"+lampIndex;
			bindings.addBinding("lamp", new RDFTermURI(lampURI));
		}
		
		public boolean subscribe() {			
			long startTime = System.nanoTime();
			subID=super.subscribe(bindings);
			long stopTime = System.nanoTime();
			Logger.log(VERBOSITY.INFO, tag , "SUBSCRIBE LAMP "+lampURI+ " "+(stopTime-startTime));
			
			return (subID != null);
		}
		
		public void terminate() {	
			synchronized(sync) {
				running = false;
				sync.notify();
			}
		}
		
		@Override
		public void run() {
			synchronized(sync) {
				running = true;	
				while(running) {
					try {
						sync.wait();
					} catch (InterruptedException e) {}
				}
			}
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
			Logger.log(VERBOSITY.INFO, tag , "LAMP NOTIFY"+lampURI+ " "+spuid+" sequence: "+sequence + " total: "+incrementLampNotifies());
			
		}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
		}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {
		}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {
		}
		
	}
	
	class RoadSubscription extends Consumer implements Runnable {		
		private String subID;
		private String roadURI ="";
		private boolean running = true;
		Bindings bindings = new Bindings();
		private Object sync = new Object();
		
		public RoadSubscription(int index) {
			super(appProfile,"ROAD");
			roadURI = "bench:Road_"+index;
			bindings.addBinding("?road", new RDFTermURI(roadURI));
		}
		
		public boolean subscribe() {
			long startTime = System.nanoTime();
			subID=super.subscribe(bindings);
			long stopTime = System.nanoTime();
			Logger.log(VERBOSITY.INFO, tag , "SUBSCRIBE ROAD "+roadURI+" "+(stopTime-startTime));
			
			return (subID != null);
		}
		
		public void terminate() {
			synchronized(sync) {
				running = false;
				sync.notify();
			}
		}
		
		@Override
		public void run() {
			synchronized(sync) {
				running = true;	
				while(running) {
					try {
						sync.wait();
					} catch (InterruptedException e) {}
				}
			}
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
			Logger.log(VERBOSITY.INFO, tag , "ROAD NOTIFY"+roadURI+ " "+spuid+" sequence: "+sequence + " total: "+incrementRoadNotifies());
			
		}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
		}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {
		}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {
		}
		
	}
	
	private boolean subscribeLamp(int roadIndex,int postIndex) {
		LampSubscription sub = new LampSubscription(roadIndex,postIndex);
		if (!sub.join()) return false;
		new Thread(sub).start();
		lampSubs.add(sub);
		return sub.subscribe();
	}
	
	private boolean subscribeRoad(int roadIndex) {
		RoadSubscription sub = new RoadSubscription(roadIndex);
		roadSubs.add(sub);
		new Thread(sub).start();
		if (!sub.join()) return false;
		return sub.subscribe();
	}
	
	private int populate(int nRoad,int nPost,int firstRoadIndex) {
		
		Producer road = new Producer(appProfile,"INSERT_ROAD");
		Producer addPost2Road = new Producer(appProfile,"ADD_POST");
		Producer sensor = new Producer(appProfile,"INSERT_SENSOR");
		Producer post = new Producer(appProfile,"INSERT_POST");
		Producer lamp = new Producer(appProfile,"INSERT_LAMP");
		Producer addSensor2post  = new Producer(appProfile,"ADD_SENSOR");
		Producer addLamp2post = new Producer(appProfile,"ADD_LAMP");
		
		if(!road.join()) return firstRoadIndex;
		if(!addPost2Road.join()) return firstRoadIndex;
		if(!post.join()) return firstRoadIndex;
		if(!sensor.join()) return firstRoadIndex;
		if(!lamp.join()) return firstRoadIndex;
		if(!addSensor2post.join()) return firstRoadIndex;
		if(!addLamp2post.join()) return firstRoadIndex;
		
		Logger.log(VERBOSITY.DEBUG, tag , "Number of roads: "+nRoad+" Posts/road: "+nPost+" First road index: "+firstRoadIndex);
		
		Bindings bindings = new Bindings();
		
		//int roadIndex = firstRoadIndex;
		
		for (int roadIndex = firstRoadIndex; roadIndex < firstRoadIndex+nRoad; roadIndex++){
		//while (nRoad>0) {
			
			String roadURI = "bench:Road_"+roadIndex;
			
			bindings.addBinding("road", new RDFTermURI(roadURI));
			
			long startTime = System.nanoTime();
			Boolean ret = road.update(bindings);
			long stopTime = System.nanoTime();
			Logger.log(VERBOSITY.INFO, tag, "INSERT ROAD "+roadURI+" "+(stopTime-startTime)+" 1");
			
			if(!ret) return firstRoadIndex;
			
			//int postNumber = nPost;
			
			for (int postIndex = 1; postIndex < nPost+1; postIndex++) {
			//while(postNumber>0) {
				//URI
				String postURI = "bench:Post_"+roadIndex+"_"+postIndex;
				String lampURI = "bench:Lamp_"+roadIndex+"_"+postIndex;				
				String temparatureURI = "bench:Temperature_"+roadIndex+"_"+postIndex;
				String presenceURI = "bench:Presence_"+roadIndex+"_"+postIndex;
							
				bindings.addBinding("post", new RDFTermURI(postURI));
				bindings.addBinding("lamp", new RDFTermURI(lampURI));
				
				//New post
				startTime = System.nanoTime();
				ret = post.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT POST "+postURI+" "+(stopTime-startTime) + " 3");				
				if(!ret) return firstRoadIndex;
				
				//Add post to road
				startTime = System.nanoTime();
				ret = addPost2Road.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT POST2ROAD "+postURI+" "+(stopTime-startTime)+ " 1");				
				if(!ret) return firstRoadIndex;
				
				//New lamp				
				startTime = System.nanoTime();
				ret = lamp.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT LAMP "+lampURI+" "+(stopTime-startTime) + " 4");				
				if(!ret) return firstRoadIndex;
				
				//Add lamp to post
				startTime = System.nanoTime();
				ret = addLamp2post.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT LAMP2POST "+lampURI+" "+(stopTime-startTime) + " 1");				
				if(!ret) return firstRoadIndex;
				
				//New temperature sensor
				bindings.addBinding("sensor", new RDFTermURI(temparatureURI));
				bindings.addBinding("type", new RDFTermURI("bench:TEMPERATURE"));
				bindings.addBinding("unit", new RDFTermURI("bench:CELSIUS"));
				bindings.addBinding("value", new RDFTermLiteral("0"));
				
				startTime = System.nanoTime();
				ret = sensor.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR "+temparatureURI+" "+(stopTime-startTime)+ " 5");				
				if(!ret) return firstRoadIndex;

				startTime = System.nanoTime();
				ret = addSensor2post.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR2POST "+temparatureURI+" "+(stopTime-startTime) + " 1");				
				if(!ret) return firstRoadIndex;
				
				//New presence sensor
				bindings.addBinding("sensor", new RDFTermURI(presenceURI));
				bindings.addBinding("type", new RDFTermURI("bench:PRESENCE"));
				bindings.addBinding("unit", new RDFTermURI("bench:BOOLEAN"));
				bindings.addBinding("value", new RDFTermLiteral("false"));
				
				startTime = System.nanoTime();
				ret = sensor.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR "+presenceURI+" "+(stopTime-startTime)+ " 5");				
				if(!ret) return firstRoadIndex;

				startTime = System.nanoTime();
				ret = addSensor2post.update(bindings);
				stopTime = System.nanoTime();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR2POST "+presenceURI+" "+(stopTime-startTime)+ " 1");				
				if(!ret) return firstRoadIndex;
			}
		}
		
		if(!road.leave()) return firstRoadIndex;
		if(!addPost2Road.leave()) return firstRoadIndex;
		if(!post.leave()) return firstRoadIndex;
		if(!sensor.leave()) return firstRoadIndex;
		if(!lamp.leave()) return firstRoadIndex;
		if(!addSensor2post.leave()) return firstRoadIndex;
		if(!addLamp2post.leave()) return firstRoadIndex;
		
		return firstRoadIndex+nRoad;
	}
	
	protected boolean updateLamp(int nRoad,int nLamp,Integer dimming) {
		String lampURI = "bench:Lamp_"+nRoad+"_"+nLamp;
		Bindings bindings = new Bindings();
		bindings.addBinding("lamp", new RDFTermURI(lampURI));
		bindings.addBinding("dimming", new RDFTermLiteral(dimming.toString()));
		
		if (!lampUpdater.join()) return false;
		
		long startTime = System.nanoTime();		
		Boolean ret = lampUpdater.update(bindings);
		long stopTime = System.nanoTime();
		
		Logger.log(VERBOSITY.INFO, tag, "UPDATE LAMP "+lampURI+" "+(stopTime-startTime));
		
		return ret && lampUpdater.leave();
	}
	
	protected boolean updateRoad(int nRoad,Integer dimming) {
		String roadURI = "bench:Road_"+nRoad;
		Bindings bindings = new Bindings();
		bindings.addBinding("?road", new RDFTermURI(roadURI));
		bindings.addBinding("?dimming", new RDFTermLiteral(dimming.toString()));
		
		if(!roadUpdater.join()) return false;
		
		long startTime = System.nanoTime();
		Boolean ret = roadUpdater.update(bindings);
		long stopTime = System.nanoTime();
		
		Logger.log(VERBOSITY.INFO, tag, "UPDATE ROAD "+roadURI+" "+(stopTime-startTime));
		
		return roadUpdater.leave() && ret;
	}
	
	private void load() {
		dataset();
		
		for (RoadPool road : roadPoll) {
			Logger.log(VERBOSITY.DEBUG, tag ,"INSERT "+ road.getNumber()+"x"+road.getSize()+ " roads ("+road.getFirstIndex()+":"+ (road.getFirstIndex()+road.getNumber()-1)+")");
			populate(road.getNumber(),road.getSize(),road.getFirstIndex());
		}
	}
	
	private void waitNotifications(int delay){
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for (LampSubscription sub : lampSubs) {
			sub.unsubscribe();
			sub.leave(); 
			sub.terminate();
		}
		for (RoadSubscription sub : roadSubs) {
			sub.unsubscribe();
			sub.leave(); 
			sub.terminate();
		}
	}
	
	private void activateSubscriptions() {
		subscribe();
		
		//SLAMP
		for (Lamp lamp : lampSubscriptions) subscribeLamp(lamp.getRoad(),lamp.getPost());
		
		//SROAD
		for (Integer index: roadSubscriptions) subscribeRoad(index);
	}
	
	public void run(boolean load, boolean reset,int delay){
		if (load) load();
		if (reset) reset();
		activateSubscriptions();
		runExperiment();
		waitNotifications(delay);
	}
}
