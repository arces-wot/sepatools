/* This class implements a JSON parser of an .sap file
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

package arces.unibo.SEPA.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.RDFTerm;
import arces.unibo.SEPA.commons.SPARQL.RDFTermLiteral;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;

/* SAP file example
 {
    "parameters" : { "path":"sparql",
		     "subscribeSecurePort":9443, "subscribePort":9000,
		     "updateSecurePort":8443, "updatePort":8000,
		     "host":"localhost"},
    "namespaces" : { "iot":"http://www.arces.unibo.it/iot#",
		     "rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#"},
    "updates": {
	"ADD_PERSON":{
	    "sparql":"INSERT DATA { ?person rdf:type iot:Person . ?person iot:hasName ?name }",
	    "forcedBindings": {
		"person" : {"type":"uri", "value":""},
		"name" : {"type":"literal", "value":""}}}
    },    
    "subscribes": {
	"CLASS_INSTANCES":{
	    "sparql":"SELECT ?s WHERE { ?s rdf:type ?class }",
	    "forcedBindings": {
		"class" : {"type":"uri", "value":""}}},
	"EVERYTHING":{	
	    "sparql":"SELECT ?s ?p ?o WHERE  { ?s ?p ?o }",
	    "forcedBindings": {}}
    }
}
*/
public class ApplicationProfile {	
	protected Logger logger = LogManager.getLogger("SAP");	
	
	protected HashMap<String,String> updateMap = new HashMap<>();
	protected HashMap<String,String> subscribeMap = new HashMap<>();
	
	protected HashMap<String,Bindings> updateBindingsMap = new HashMap<>();
	protected HashMap<String,Bindings> subscribeBindingsMap = new HashMap<>();
	
	protected HashMap<String,String> namespaceMap = new HashMap<>();
	
	protected Parameters params = new Parameters();
	
	protected boolean loaded = false;
	
	public class Parameters {
		private String url = "mml.arces.unibo.it";
		private int updatePort = 8000;
		private int subscribePort = 9000;
		private String path = "/sparql";
		private int updateSecurePort = 8443;
		private int subscribeSecurePort = 9443;
		
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public int getUpdatePort() {
			return updatePort;
		}
		public void setUpdatePort(int port) {
			this.updatePort = port;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public void setSubscribePort(int port) {
			this.subscribePort = port;
		}
		public int getSubscribePort() {
			return subscribePort;
		}
		public int getUpdateSecurePort() {
			return updateSecurePort;
		}
		public void setUpdateSecurePort(int updateSecurePort) {
			this.updateSecurePort = updateSecurePort;
		}
		public int getSubscribeSecurePort() {
			return subscribeSecurePort;
		}
		public void setSubscribeSecurePort(int subscribeSecurePort) {
			this.subscribeSecurePort = subscribeSecurePort;
		}
	}
	
	public String qName(String uri){
		if (uri == null) return null;
		for (String prefix : namespaceMap.keySet()) {
			if (uri.startsWith(namespaceMap.get(prefix))) return uri.replace(namespaceMap.get(prefix), prefix+":");
		}
		return uri;
	}
	
	public Set<String> getSubscribeIds() {return subscribeMap.keySet();}
	public Set<String> getUpdateIds() {return updateMap.keySet();}
	public Set<String> getPrefixes() {return namespaceMap.keySet();}
	public Parameters getParameters() {return params;}
	
	public String getNamespaceURI(String prefix) {
		String ret = namespaceMap.get(prefix);
		if (ret == null) logger.error("Prefix " + prefix + " NOT FOUND");
		return ret;
	}
	public Bindings subscribeBindings(String id) {
		return subscribeBindingsMap.get(id);
	}
	
	public Bindings updateBindings(String id) {
		return updateBindingsMap.get(id);
	}
	
	public String subscribe(String id) {
		if (!subscribeMap.containsKey(id)) {
			logger.error("SUBSCRIBE ID <" + id + "> NOT FOUND");
			return null;
		}
		return subscribeMap.get(id);
	}
	
	public String update(String id) {
		if (!updateMap.containsKey(id)) {
			logger.error("UPDATE ID <" + id + "> NOT FOUND");
			return null;
		}
		return updateMap.get(id);
	}
		
	public synchronized boolean load(String fileName){
		
		loaded = false;
		
		subscribeMap.clear();
		subscribeBindingsMap.clear();
		updateMap.clear();
		updateBindingsMap.clear();
		namespaceMap.clear();
		
		File inputFile = new File(fileName);
		Reader reader = null;
		try {
			reader = new FileReader(inputFile);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
			return false;
		}
		JsonObject doc = new JsonParser().parse(reader).getAsJsonObject();
		
		if (doc == null) {
			logger.error("Failed to parse "+fileName);
			return false;
		}

		/*		
		"parameters" : { "path":"sparql",
		     "subscribeSecurePort":9443, "subscribePort":9000,
		     "updateSecurePort":8443, "updatePort":8000,
		     "host":"localhost"},
		*/
		JsonElement elem;
		JsonObject obj;
		if ((obj = doc.get("parameters").getAsJsonObject()) !=null){
			if ((elem = obj.get("path")) != null) params.setPath(elem.getAsString());
			if ((elem = obj.get("subscribeSecurePort")) != null) params.setSubscribeSecurePort(elem.getAsInt());
			if ((elem = obj.get("subscribePort")) != null) params.setSubscribePort(elem.getAsInt());
			if ((elem = obj.get("updateSecurePort")) != null) params.setUpdateSecurePort(elem.getAsInt());
			if ((elem = obj.get("updatePort")) != null) params.setUpdatePort(elem.getAsInt());
			if ((elem = obj.get("host")) != null) params.setUrl(elem.getAsString());
		}
		else logger.warn("Parameters key not found...using defaults");

		/* "namespaces" : { "iot":"http://www.arces.unibo.it/iot#",
		     "rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#"},
		*/
		if ((obj = doc.get("namespaces").getAsJsonObject()) !=null){
			for(Entry<String, JsonElement> entry :obj.entrySet()) namespaceMap.put(entry.getKey(), entry.getValue().getAsString());
		}
		else logger.warn("Namespaces key not found");
		
		/*
   		"updates": {
			"ADD_PERSON":{
	    		"sparql":"INSERT DATA { ?person rdf:type iot:Person . ?person iot:hasName ?name }",
	    		"forcedBindings": {
					"person" : {"type":"uri", "value":""},
					"name" : {"type":"literal", "value":""}}}
   		},    
		 */
		if ((obj = doc.get("updates").getAsJsonObject()) !=null){
			for(Entry<String, JsonElement> entry :obj.entrySet()) {
				JsonObject update = entry.getValue().getAsJsonObject();
				if (update.get("sparql")!=null) updateMap.put(entry.getKey(), update.get("sparql").getAsString());
				if (update.get("forcedBindings")!=null) {
					Bindings bindings = new Bindings();
					for(Entry<String, JsonElement> binding : update.get("forcedBindings").getAsJsonObject().entrySet()) {
						JsonObject bindingValue = binding.getValue().getAsJsonObject();
						if (bindingValue.get("type") == null) continue;
						if (bindingValue.get("value") == null) continue;
						RDFTerm value = null;
						if (bindingValue.get("type").getAsString().equals("uri")) value = new RDFTermURI(bindingValue.get("value").getAsString());
						else if (bindingValue.get("type").getAsString().equals("literal")) value = new RDFTermLiteral(bindingValue.get("value").getAsString());
						
						if (value != null) bindings.addBinding(binding.getKey(), value);
						else logger.warn("Binding type must be uri or literal");
					}
					updateBindingsMap.put(entry.getKey(), bindings);
				}
			}
		}
		
		/*
   		"subscribes": {
			"CLASS_INSTANCES":{
	    		"sparql":"SELECT ?s WHERE { ?s rdf:type ?class }",
	    		"forcedBindings": {
					"class" : {"type":"uri", "value":""}}},
		"EVERYTHING":{	
	    	"sparql":"SELECT ?s ?p ?o WHERE  { ?s ?p ?o }",
	    	"forcedBindings": {}}
		 */
		if ((obj = doc.get("subscribes").getAsJsonObject()) !=null){
			for(Entry<String, JsonElement> entry :obj.entrySet()) {
				JsonObject subscribe = entry.getValue().getAsJsonObject();
				if (subscribe.get("sparql")!=null) subscribeMap.put(entry.getKey(), subscribe.get("sparql").getAsString());
				if (subscribe.get("forcedBindings")!=null) {
					Bindings bindings = new Bindings();
					for(Entry<String, JsonElement> binding : subscribe.get("forcedBindings").getAsJsonObject().entrySet()) {
						JsonObject bindingValue = binding.getValue().getAsJsonObject();
						if (bindingValue.get("type") == null) continue;
						if (bindingValue.get("value") == null) continue;
						RDFTerm value = null;
						if (bindingValue.get("type").getAsString().equals("uri")) value = new RDFTermURI(bindingValue.get("value").getAsString());
						else if (bindingValue.get("type").getAsString().equals("literal")) value = new RDFTermLiteral(bindingValue.get("value").getAsString());
						
						if (value != null) bindings.addBinding(binding.getKey(), value);
						else logger.warn("Binding type must be uri or literal");
					}
					subscribeBindingsMap.put(entry.getKey(), bindings);
				}
			}
		}

		loaded = true;
		
		return true;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
}
