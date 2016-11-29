package arces.unibo.SEPA;

import java.util.HashMap;
import java.util.Set;

import arces.unibo.KPI.KPICore;
import arces.unibo.KPI.SIBResponse;
import arces.unibo.SEPA.Logger.VERBOSITY;
import arces.unibo.SEPA.SPARQLApplicationProfile.Parameters;

public abstract class Client implements IClient{	
	protected HashMap<String,String> URI2PrefixMap = new HashMap<String,String>();
	protected HashMap<String,String> prefix2URIMap = new HashMap<String,String>();
	protected KPICore kp = null;
	protected SIBResponse ret;
	private boolean joined = false;
	
	public void addNamespace(String prefix,String uri){
		if (prefix2URIMap.containsKey(prefix)) removeNamespace(prefix);
		URI2PrefixMap.put(uri, prefix);
		prefix2URIMap.put(prefix, uri);
	}
	
	public void removeNamespace(String prefix){
		if (!prefix2URIMap.containsKey(prefix)) return;
		String rmURI = prefix2URIMap.get(prefix);
		URI2PrefixMap.remove(rmURI);
		prefix2URIMap.remove(prefix);
	}
	
	public void clearNamespaces() {
		URI2PrefixMap.clear();
		prefix2URIMap.clear();
	}
	
	protected String prefixes() {
		String ret = "";
		for (String prefix : prefix2URIMap.keySet())
			ret += "PREFIX " + prefix + ":<" + prefix2URIMap.get(prefix) + "> ";
		return ret;
	}
	
	public Client(String SIB_IP,int SIB_PORT,String SIB_NAME){
		Logger.log(VERBOSITY.DEBUG,"SEPA","CLIENT Created IP:"+SIB_IP+" Port:"+SIB_PORT+" Name:"+SIB_NAME);
		kp = new KPICore(SIB_IP, SIB_PORT, SIB_NAME);	
	}
	
	public Client(SPARQLApplicationProfile appProfile){
		if (!appProfile.isLoaded()) Logger.log(VERBOSITY.WARNING,"SEPA","CLIENT running with default parameters. No application profile loaded");
		
		Parameters args = appProfile.getParameters();
		Logger.log(VERBOSITY.DEBUG,"SEPA","CLIENT Created IP:"+args.getUrl()+" Port:"+args.getPort()+" Name:"+args.getName());
		
		kp = new KPICore(args.getUrl(), args.getPort(),args.getName());
		
		Set<String> prefixes = appProfile.getPrefixes();
		for (String prefix : prefixes) addNamespace(prefix,appProfile.getNamespaceURI(prefix));
	}
	
	public boolean join() {
		joined = kp.join().isConfirmed();
		return joined;
	}
	
	protected boolean isJoined() {return joined;}
	
	public boolean leave() {
		if (!joined) return false;
		
		joined = !kp.leave().isConfirmed();
		
		return !joined;
	}
	
	protected String replaceBindings(String sparql, Bindings bindings){
		if (bindings == null) return sparql;
		
		String replacedSparql = String.format("%s", sparql);
		String selectPattern = "";
		
		if (sparql.contains("SELECT")) {
			selectPattern = replacedSparql.substring(0, sparql.indexOf('{'));
			replacedSparql = replacedSparql.substring(sparql.indexOf('{'), replacedSparql.length());
		}
		for (String var : bindings.getVariables()) {
			if (bindings.getBindingValue(var) == null) continue;
			if (bindings.getBindingValue(var).isLiteral()) 
				replacedSparql = replacedSparql.replace(var,"\""+bindings.getBindingValue(var).getValue()+"\"");
			else	
				replacedSparql = replacedSparql.replace(var,bindings.getBindingValue(var).getValue());
			
			selectPattern = selectPattern.replace(var, "");
		}
		
		return selectPattern+replacedSparql;
	}
	
	public boolean loadRDFXMLOntology(String xml) {
		if (!joined) return false;
		SIBResponse ret  = kp.insert_rdf_xml(xml);
		return ret.isConfirmed();
	}
}
