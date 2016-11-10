package arces.unibo.SEPA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import arces.unibo.KPI.SSAP_sparql_response;


public class BindingsResults {
	private ArrayList<Bindings> addedBindings = null;
	private ArrayList<Bindings> removedBindings = null;
	private ArrayList<String> variables = null;
	
	public BindingsResults(SSAP_sparql_response addedBindings,SSAP_sparql_response removedBindings,HashMap<String,String> URI2PrefixMap){
		this.addedBindings = getBindings(addedBindings,URI2PrefixMap,true);
		this.removedBindings = getBindings(removedBindings,URI2PrefixMap,false);
		variables = new ArrayList<String>();
		if (addedBindings != null) 
			for(String var : addedBindings.getVariablesNames()) variables.add("?"+var);
		else if (removedBindings != null)
			for(String var : removedBindings.getVariablesNames()) variables.add("?"+var);
	}
	
	public ArrayList<Bindings> getAddedBindings() {
		return addedBindings;
	}
	
	public ArrayList<Bindings> getRemovedBindings() {
		return removedBindings;
	}
	
	public ArrayList<String> getVariables(){
		return variables;
	}
	
	private ArrayList<Bindings> getBindings(SSAP_sparql_response sparl,HashMap<String,String> URI2PrefixMap,boolean added) {
		ArrayList<Bindings> ret = new ArrayList<Bindings>();
		
		if (sparl == null) return ret;
		
		for(Vector<String[]> result : sparl.getResults()) {
			Bindings bindings = new Bindings();
			for(String[] variable : result) {
				String name = SSAP_sparql_response.getCellName(variable);
				String value = SSAP_sparql_response.getCellValue(variable);
				boolean uri = SSAP_sparql_response.getCellCategory(variable).equals("uri");
				
				if (uri)
					bindings.addBinding("?"+name,new BindingURIValue(value,URI2PrefixMap,added));
				else
					bindings.addBinding("?"+name,new BindingLiteralValue(value,added));
			}
			ret.add(bindings);
		}
		return ret;
	}
	
	@Override
	public String toString(){
		String ret = "\nADDED BINDINGS:\n";
		if (addedBindings != null)
			for(Bindings bindings : addedBindings) ret += "\t["+ bindings.toString() + "]\n";

		ret += "REMOVED BINDINGS:\n";
		if (removedBindings != null)
			for(Bindings bindings : removedBindings) ret += "\t["+ bindings.toString() + "]\n";
		
		return ret;
	}
}
