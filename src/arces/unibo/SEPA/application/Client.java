package arces.unibo.SEPA.application;

import java.util.HashMap;
import java.util.Set;

import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.application.SPARQLApplicationProfile.Parameters;
import arces.unibo.SEPA.client.SPARQLSEProtocolClient;
import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public abstract class Client implements IClient {	
	protected HashMap<String,String> URI2PrefixMap = new HashMap<String,String>();
	protected HashMap<String,String> prefix2URIMap = new HashMap<String,String>();
	protected SPARQLSEProtocolClient protocolClient = null;
	
	private static String tag ="SEPA CLIENT";
	
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
	
	public Client(String url,int updatePort,int subscribePort,String path){
		Logger.log(VERBOSITY.DEBUG,tag,"Created Authority:"+url+" Update port:"+updatePort+" Subscribe port:"+subscribePort+ " Path: "+path);
		protocolClient = new SPARQLSEProtocolClient(url, updatePort, subscribePort,path);	
	}
	
	public boolean join() {
		return true;
	}
	
	public boolean leave() {
		return true;
	}
	
	public Client(SPARQLApplicationProfile appProfile){
		if (appProfile == null) {
			Logger.log(VERBOSITY.FATAL,tag,"Application profile is null. Client cannot be initialized");
			return;
		}
		if (!appProfile.isLoaded()) Logger.log(VERBOSITY.WARNING,tag,"Running with default parameters. No application profile loaded");
		
		Parameters args = appProfile.getParameters();
		Logger.log(VERBOSITY.DEBUG,tag,"Created Authority:"+args.getUrl()+" Update port:"+args.getUpdatePort()+" Subscribe port:"+args.getSubscribePort()+ " Path: "+args.getPath());
		
		protocolClient = new SPARQLSEProtocolClient(args.getUrl(), args.getUpdatePort(),args.getSubscribePort(),args.getPath());
		
		Set<String> prefixes = appProfile.getPrefixes();
		for (String prefix : prefixes) addNamespace(prefix,appProfile.getNamespaceURI(prefix));
	}
	
	protected String replaceBindings(String sparql, SPARQLQuerySolution bindings){
		if (bindings == null) return sparql;
		
		String replacedSparql = String.format("%s", sparql);
		String selectPattern = "";
		
		if (sparql.toUpperCase().contains("SELECT")) {
			selectPattern = replacedSparql.substring(0, sparql.indexOf('{'));
			replacedSparql = replacedSparql.substring(sparql.indexOf('{'), replacedSparql.length());
		}
		for (String var : bindings.getVariables()) {
			if (bindings.getBindingValue(var) == null) continue;
			if (bindings.isLiteral(var)) 
				replacedSparql = replacedSparql.replace("?"+var,"\""+bindings.getBindingValue(var)+"\"");
			else	
				replacedSparql = replacedSparql.replace("?"+var,bindings.getBindingValue(var));
			
			selectPattern = selectPattern.replace("?"+var, "");
		}
		
		return selectPattern+replacedSparql;
	}
}
