package arces.unibo.SEPA;

import java.util.HashMap;
import java.util.Set;

public class Bindings {
	HashMap<String,BindingValue> result;
	
	public Bindings() {
		result = new HashMap<String,BindingValue>();
	}
	
	public void addBinding(String variable,BindingValue value){
		result.put(variable, value);
	}
	
	public BindingValue getBindingValue(String variable) {
		if (result.containsKey(variable)) {
			return result.get(variable);
		}
		return null;
	}
	
	public int size() {
		return result.size();
	}
	
	public Set<String> getVariables() {
		return result.keySet();
	}
	
	@Override
	public String toString(){
		String toString = "";
		
		if (result == null) return toString;
		if (result.keySet() == null) return toString;
		
		Set<String> vars = result.keySet();
		for (String var : vars) toString += var +"=" + result.get(var).getValue() + ";";
		
		return toString;
	}
}
