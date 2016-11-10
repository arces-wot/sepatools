package arces.unibo.benchmark;

import java.util.ArrayList;
import java.util.Vector;

import arces.unibo.SEPA.BindingLiteralValue;
import arces.unibo.SEPA.BindingURIValue;
import arces.unibo.SEPA.Bindings;
import arces.unibo.SEPA.BindingsResults;
import arces.unibo.SEPA.Consumer;
import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Producer;
import arces.unibo.SEPA.SPARQLApplicationProfile;
import arces.unibo.SEPA.Logger.VERBOSITY;

public abstract class SmartLightingBenchmark {
	//Benchmark definition
	private Vector<RoadPool> roads = new Vector<RoadPool>();
	private Vector<Integer> roadSubscriptions = new Vector<Integer>();
	private Vector<Lamp> lampSubscriptions = new Vector<Lamp>();

	private Vector<RoadSubscription> roadSubs = new Vector<RoadSubscription>();
	private Vector<LampSubscription> lampSubs = new Vector<LampSubscription>();
	
	private Producer lampUpdater;
	private Producer roadUpdater;
	private String tag = "SmartLightingBenchmark";

	private int lampNotifyN = 0;
	private int roadNotifyN = 0;
	private int nRoads = 0;
	
	public abstract void reset();
	public abstract void runExperiment();
	
	private class RoadPool {
		private final int size;
		private final int number;
		
		public RoadPool(int number,int size) {
			this.size = size;
			this.number = number;
		}

		public int getSize() {
			return size;
		}

		public int getNumber() {
			return number;
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
	public void addRoad(int size) {
		roads.add(new RoadPool(1,size));
	}
	public void addRoads(int number,int size) {
		roads.add(new RoadPool(number,size));
	}
		
	public int roadNumber() {
		return nRoads;
	}
	
	public SmartLightingBenchmark() {
		SPARQLApplicationProfile.load("LightingBenchmark.xml");
		Logger.registerTag(tag);
		lampUpdater = new Producer("UPDATE_LAMP");
		roadUpdater = new Producer("UPDATE_ROAD");
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
		private String subID ="";
		private String lampURI = "";
		private boolean running = true;
		private Bindings bindings = new Bindings();
		private String tag ="LampSubscription";
		private Object sync = new Object();
		
		public LampSubscription(int roadIndex,int lampIndex) {
			super("LAMP");
			lampURI = "bench:Lamp_"+roadIndex+"_"+lampIndex;
			bindings.addBinding("?lamp", new BindingURIValue(lampURI));
			Logger.registerTag(tag);
		}
		
		public boolean subscribe() {			
			long startTime = System.currentTimeMillis();
			subID=super.subscribe(bindings);
			long stopTime = System.currentTimeMillis();
			Logger.log(VERBOSITY.INFO, tag , "Subscribe LAMP "+subID+ " "+(stopTime-startTime));
			
			if (subID!=null) if (!subID.equals("")) return true;
			return false;
		}

		@Override
		public void notify(BindingsResults notify) {
			Logger.log(VERBOSITY.INFO, tag, "Notify LAMP "+lampURI+" "+incrementLampNotifies());
		}

		@Override
		public void notifyAdded(ArrayList<Bindings> bindingsResults) {}

		@Override
		public void notifyRemoved(ArrayList<Bindings> bindingsResults) {}

		@Override
		public void notifyFirst(ArrayList<Bindings> bindingsResults) {}
		
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
		
	}
	
	class RoadSubscription extends Consumer implements Runnable {		
		private String subID = "";
		private String roadURI ="";
		private boolean running = true;
		Bindings bindings = new Bindings();
		private String tag = "RoadSubscription";
		private Object sync = new Object();
		
		public RoadSubscription(int index) {
			super("ROAD");
			roadURI = "bench:Road_"+index;
			bindings.addBinding("?road", new BindingURIValue(roadURI));
			Logger.registerTag(tag);
		}
		
		public boolean subscribe() {
			long startTime = System.currentTimeMillis();
			subID=super.subscribe(bindings);
			long stopTime = System.currentTimeMillis();
			Logger.log(VERBOSITY.INFO, tag , " Subscribe ROAD "+subID+" "+(stopTime-startTime));
			
			if (subID!=null) if (!subID.equals("")) return true;
			return false;
		}

		@Override
		public void notify(BindingsResults notify) {
			Logger.log(VERBOSITY.INFO,tag, "Notify ROAD "+roadURI+" "+incrementRoadNotifies());
		}

		@Override
		public void notifyAdded(ArrayList<Bindings> bindingsResults) {}

		@Override
		public void notifyRemoved(ArrayList<Bindings> bindingsResults) {}

		@Override
		public void notifyFirst(ArrayList<Bindings> bindingsResults) {}
		
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
		
	}
	
	protected boolean subscribeLamp(int roadIndex,int postIndex) {
		LampSubscription sub = new LampSubscription(roadIndex,postIndex);
		if (!sub.join()) return false;
		new Thread(sub).start();
		lampSubs.add(sub);
		return sub.subscribe();
	}
	
	protected boolean subscribeRoad(int roadIndex) {
		RoadSubscription sub = new RoadSubscription(roadIndex);
		roadSubs.add(sub);
		new Thread(sub).start();
		if (!sub.join()) return false;
		return sub.subscribe();
	}
	
	protected int populate(int nRoad,int nPost,int firstRoadIndex) {
		
		Producer road = new Producer("INSERT_ROAD");
		Producer addPost2Road = new Producer("ADD_POST");
		Producer sensor = new Producer("INSERT_SENSOR");
		Producer post = new Producer("INSERT_POST");
		Producer lamp = new Producer("INSERT_LAMP");
		Producer addSensor2post  = new Producer("ADD_SENSOR");
		Producer addLamp2post = new Producer("ADD_LAMP");
		
		if(!road.join()) return firstRoadIndex;
		if(!addPost2Road.join()) return firstRoadIndex;
		if(!post.join()) return firstRoadIndex;
		if(!sensor.join()) return firstRoadIndex;
		if(!lamp.join()) return firstRoadIndex;
		if(!addSensor2post.join()) return firstRoadIndex;
		if(!addLamp2post.join()) return firstRoadIndex;
		
		Logger.log(VERBOSITY.DEBUG, tag , "Number of roads: "+nRoad+" Posts/road: "+nPost+" First road index: "+firstRoadIndex);
		
		Bindings bindings = new Bindings();
		
		int roadIndex = firstRoadIndex;
		
		while (nRoad>0) {
			
			String roadURI = "bench:Road_"+roadIndex;
			
			bindings.addBinding("?road", new BindingURIValue(roadURI));
			
			long startTime = System.currentTimeMillis();
			boolean ret = road.update(bindings);
			long stopTime = System.currentTimeMillis();
			Logger.log(VERBOSITY.INFO, tag, "INSERT ROAD "+roadURI+" "+(stopTime-startTime));
			
			if(!ret) return firstRoadIndex;
			
			int postNumber = nPost;
			
			while(postNumber>0) {
				//URI
				String postURI = "bench:Post_"+roadIndex+"_"+postNumber;
				String lampURI = "bench:Lamp_"+roadIndex+"_"+postNumber;				
				String temparatureURI = "bench:Temperature_"+roadIndex+"_"+postNumber;
				String presenceURI = "bench:Presence_"+roadIndex+"_"+postNumber;
							
				bindings.addBinding("?post", new BindingURIValue(postURI));
				bindings.addBinding("?lamp", new BindingURIValue(lampURI));
				
				//New post
				startTime = System.currentTimeMillis();
				ret = post.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT POST "+postURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;
				
				//Add post to road
				startTime = System.currentTimeMillis();
				ret = addPost2Road.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT POST2ROAD "+postURI+" "+roadURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;
				
				//New lamp				
				startTime = System.currentTimeMillis();
				ret = lamp.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT LAMP "+lampURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;
				
				//Add lamp to post
				startTime = System.currentTimeMillis();
				ret = addLamp2post.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT LAMP2POST "+lampURI+" "+postURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;
				
				//New temperature sensor
				bindings.addBinding("?sensor", new BindingURIValue(temparatureURI));
				bindings.addBinding("?type", new BindingURIValue("bench:TEMPERATURE"));
				bindings.addBinding("?unit", new BindingURIValue("bench:CELSIUS"));
				bindings.addBinding("?value", new BindingLiteralValue("0"));
				
				startTime = System.currentTimeMillis();
				ret = sensor.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR "+temparatureURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;

				startTime = System.currentTimeMillis();
				ret = addSensor2post.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR2POST "+temparatureURI+" "+postURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;
				
				//New presence sensor
				bindings.addBinding("?sensor", new BindingURIValue(presenceURI));
				bindings.addBinding("?type", new BindingURIValue("bench:PRESENCE"));
				bindings.addBinding("?unit", new BindingURIValue("bench:BOOLEAN"));
				bindings.addBinding("?value", new BindingLiteralValue("false"));
				
				startTime = System.currentTimeMillis();
				ret = sensor.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR "+presenceURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;

				startTime = System.currentTimeMillis();
				ret = addSensor2post.update(bindings);
				stopTime = System.currentTimeMillis();
				Logger.log(VERBOSITY.INFO, tag, "INSERT SENSOR2POST "+presenceURI+" "+postURI+" "+(stopTime-startTime));				
				if(!ret) return firstRoadIndex;
				
				postNumber--;	
			}
			
			nRoad--;
			roadIndex++;
		}
		
		if(!road.leave()) return firstRoadIndex;
		if(!addPost2Road.leave()) return firstRoadIndex;
		if(!post.leave()) return firstRoadIndex;
		if(!sensor.leave()) return firstRoadIndex;
		if(!lamp.leave()) return firstRoadIndex;
		if(!addSensor2post.leave()) return firstRoadIndex;
		if(!addLamp2post.leave()) return firstRoadIndex;
		
		return roadIndex;
	}
	
	protected boolean updateLamp(int nRoad,int nLamp,Integer dimming) {
		String lampURI = "bench:Lamp_"+nRoad+"_"+nLamp;
		Bindings bindings = new Bindings();
		bindings.addBinding("?lamp", new BindingURIValue(lampURI));
		bindings.addBinding("?dimming", new BindingLiteralValue(dimming.toString()));
		
		if (!lampUpdater.join()) return false;
		
		long startTime = System.currentTimeMillis();		
		boolean ret = lampUpdater.update(bindings);
		long stopTime = System.currentTimeMillis();
		
		Logger.log(VERBOSITY.INFO, tag, "UPDATE LAMP "+lampURI+" "+(stopTime-startTime));
		
		return ret && lampUpdater.leave();
	}
	
	protected boolean updateRoad(int nRoad,Integer dimming) {
		String roadURI = "bench:Road_"+nRoad;
		Bindings bindings = new Bindings();
		bindings.addBinding("?road", new BindingURIValue(roadURI));
		bindings.addBinding("?dimming", new BindingLiteralValue(dimming.toString()));
		
		if(!roadUpdater.join()) return false;
		
		long startTime = System.currentTimeMillis();
		boolean ret = roadUpdater.update(bindings);
		long stopTime = System.currentTimeMillis();
		
		Logger.log(VERBOSITY.INFO, tag, "UPDATE ROAD "+roadURI+" "+(stopTime-startTime));
		
		return roadUpdater.leave() && ret;
	}
	
	private void load() {
		int roadIndex = 1;
		for (RoadPool road : roads) {
			Logger.log(VERBOSITY.DEBUG, tag ,"INSERT "+ road.getNumber()+"x"+road.getSize()+ " roads ("+roadIndex+":"+ (roadIndex+road.getNumber()-1)+")");
			roadIndex = populate(road.getNumber(),road.getSize(),roadIndex);
			nRoads = nRoads +  road.getNumber();
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
