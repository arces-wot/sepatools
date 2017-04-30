package arces.unibo.SEPA.client.tools;

public class RoadExperiment extends SmartLightingBenchmark {
	
	//Data set
	protected int roads[] = {100,100,100,10};
	protected int roadSizes[] = {10,25,50,100};
	
	//Road subscriptions
	protected int roadSubscriptionRoads[] = {6,105,204,308};
	
	//Lamp subscriptions
	protected int lampSubscriptionRoads[][] = {{1,5},{101,104},{201,203},{301,307}};
	protected int lampSubscriptionLamps[][] = {{1,10},{1,25},{1,50},{1,100}};
	
	public RoadExperiment() {
		super();	
	}

	@Override
	public void dataset() {
		//Data set creation
		int roadIndex = firstRoadIndex;	
		nRoads = 0;
		for (int i=0; i < roads.length; i++) {
			roadIndex = addRoads(roads[i],roadSizes[i],roadIndex);	
			nRoads = nRoads + roads[i];
		}
	}

	@Override
	public void subscribe() {
		//Road subscription
		for (int i=0; i < roadSubscriptionRoads.length; i++) addRoadSubscription(roadSubscriptionRoads[i]);
		
		//Lamp subscriptions
		for (int i=0; i < lampSubscriptionRoads.length; i++) 
			for (int X = lampSubscriptionRoads[i][0]; X < lampSubscriptionRoads[i][1]+1; X++)
				for (int Y = lampSubscriptionLamps[i][0]; Y < lampSubscriptionLamps[i][1]+1; Y++) addLampSubscription(X,Y);		
	}
	
	@Override
	public void runExperiment() {
		for (int road = 1; road < nRoads+1; road++) updateRoad(road,new Integer(100));
	}

	@Override
	public void reset() {
		for (int road = 1; road < nRoads+1; road++) updateRoad(road,new Integer(0));
	}
	
	public static void main(String[] args) {
		RoadExperiment benchmark = new RoadExperiment();
		benchmark.run(true,true,5000);
	}

}
