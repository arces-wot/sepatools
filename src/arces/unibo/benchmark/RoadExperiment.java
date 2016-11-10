package arces.unibo.benchmark;

import arces.unibo.SEPA.Logger;

public class RoadExperiment extends SmartLightingBenchmark {

	protected String tag ="RoadExp";

	public RoadExperiment() {
		super();
		
		Logger.registerTag(tag);
		
		//Dataset
		addRoads(1,10);
		addRoads(1,25);
		addRoads(1,50);
		addRoads(1,100);
		
		//Road subscriptions
		addRoadSubscription(6);
		addRoadSubscription(105);
		addRoadSubscription(204);
		addRoadSubscription(308);
		
		//Lamp subscriptions
		for (int X = 1; X < 6; X++) 
			for (int Y = 1; Y < 11; Y++) addLampSubscription(X,Y);
		for (int X = 101; X < 105; X++) 
			for (int Y = 1; Y < 26; Y++) addLampSubscription(X,Y);
		for (int X = 201; X < 204; X++) 
			for (int Y = 1; Y < 51; Y++) addLampSubscription(X,Y);
		for (int X = 301; X < 308; X++) 
			for (int Y = 1; Y < 101; Y++) addLampSubscription(X,Y);
	}

	@Override
	public void runExperiment() {
		for (int road = 1; road < 311; road++) updateRoad(road,new Integer(100));
	}

	@Override
	public void reset() {
		for (int road = 1; road < 311; road++) updateRoad(road,new Integer(0));
	}
	
	public static void main(String[] args) {
		Logger.enableConsoleLog();
		Logger.enableFileLog();
		
		RoadExperiment benchmark = new RoadExperiment();
		benchmark.run(true,true,5000);
	}
}
