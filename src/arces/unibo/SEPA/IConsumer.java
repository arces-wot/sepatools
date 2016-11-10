package arces.unibo.SEPA;

import java.util.ArrayList;

public interface IConsumer extends IClient{	
	public String subscribe(Bindings forcedBindings);
	public boolean unsubscribe();
	
	public void notify(BindingsResults notify);
	public void notifyAdded(ArrayList<Bindings> bindingsResults);
	public void notifyRemoved(ArrayList<Bindings> bindingsResults);
	public void notifyFirst(ArrayList<Bindings> bindingsResults);
}
