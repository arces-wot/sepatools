package arces.unibo.examples;

public class PopulateExperiment extends SmartLightingBenchmark {
	protected String tag ="LampExp";
	protected static SmartLightingBenchmark benchmark = new PopulateExperiment();
	
	//Data set
	protected int roads[] = {100,100,100,10};
	protected int roadSizes[] = {10,25,50,100};
	
	//Road subscriptions
	protected int roadSubscriptionRoads[] = {};
	
	//Lamp subscriptions
	protected int lampSubscriptionRoads[][] ={};
	protected int lampSubscriptionLamps[][] ={};
	
	@Override
	public void reset() {
		// TODO Auto-generated method stub	
	}

	@Override
	public void runExperiment() {
		// TODO Auto-generated method stub		
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
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		benchmark.run(true,true,5000);
	}
}
