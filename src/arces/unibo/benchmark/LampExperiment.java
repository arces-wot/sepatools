package arces.unibo.benchmark;

public class LampExperiment extends RoadExperiment {
	protected String tag ="LampExp";
	
	@Override
	public void runExperiment() {
		for (int raodIndex = 1; raodIndex < 311; raodIndex++) updateLamp(raodIndex, 1,new Integer(100));
	}
}
