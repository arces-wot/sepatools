package arces.unibo.SUBEngine;

import java.util.Set;

/**
 * This class represents the content of a SEPA notification
 * 
 * It includes the added and removed bindings since the previous notification
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class ARBindingsResults {
	private SPARQLBindingsResults addedBindings = null;
	private SPARQLBindingsResults removedBindings = null;
	private Set<String> variables = null;
	
	public ARBindingsResults(SPARQLBindingsResults added,SPARQLBindingsResults removed) {
		this.addedBindings =added;
		this.removedBindings = removed;
		variables.addAll(added.getVariables());
		variables.addAll(removed.getVariables());
	}
	
	public SPARQLBindingsResults getAddedBindings() {
		return addedBindings;
	}
	
	public SPARQLBindingsResults getRemovedBindings() {
		return removedBindings;
	}
	
	public Set<String> getVariables(){
		return variables;
	}
}
