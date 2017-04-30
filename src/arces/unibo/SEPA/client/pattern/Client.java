/* This class abstracts a client of the SEPA Application Design Pattern
Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.client.pattern;

import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.client.api.ClientProperties;
import arces.unibo.SEPA.client.api.SPARQL11SEProtocol;
import arces.unibo.SEPA.client.pattern.ApplicationProfile.Parameters;
import arces.unibo.SEPA.commons.SPARQL.Bindings;

public abstract class Client implements IClient {	
	protected HashMap<String,String> URI2PrefixMap = new HashMap<String,String>();
	protected HashMap<String,String> prefix2URIMap = new HashMap<String,String>();
	protected SPARQL11SEProtocol protocolClient = null;
	
	private static final Logger logger = LogManager.getLogger("Client");
	
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
		logger.debug("Opening connection to SEPA engine:"+url+" Update port:"+updatePort+" Subscribe port:"+subscribePort+ " Path: "+path);
		ClientProperties properties = new ClientProperties("client.properties");
		protocolClient = new SPARQL11SEProtocol(properties);	
		logger.info(protocolClient.toString());
	}
	
	public boolean join() {
		return true;
	}
	
	public boolean leave() {
		return true;
	}
	
	public Client(ApplicationProfile appProfile){
		if (appProfile == null) {
			logger.fatal("Application profile is null. Client cannot be initialized");
			return;
		}
		if (!appProfile.isLoaded()) logger.warn("Running with default parameters. No application profile loaded");
		
		Parameters args = appProfile.getParameters();
		logger.debug("Created Authority:"+args.getUrl()+" Update port:"+args.getUpdatePort()+" Subscribe port:"+args.getSubscribePort()+ " Path: "+args.getPath());
		
		ClientProperties properties = new ClientProperties("client.properties");
		protocolClient = new SPARQL11SEProtocol(properties);
		logger.info(protocolClient.toString());
		
		Set<String> prefixes = appProfile.getPrefixes();
		for (String prefix : prefixes) addNamespace(prefix,appProfile.getNamespaceURI(prefix));
	}
	
	protected String replaceBindings(String sparql, Bindings bindings){
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
				replacedSparql = replacedSparql.replace("?"+var,"\""+fixLiteralTerms(bindings.getBindingValue(var))+"\"");
			else	
				replacedSparql = replacedSparql.replace("?"+var,bindings.getBindingValue(var));
			
			selectPattern = selectPattern.replace("?"+var, "");
		}
		
		return selectPattern+replacedSparql;
	}
	
	protected String fixLiteralTerms(String s) {
		return s.replace("\"", "\\\"");
	}
}
