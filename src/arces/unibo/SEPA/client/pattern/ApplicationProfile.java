/* This class implements a JSON parser of an .sap file
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
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

	protected boolean loaded = false;
	private JsonObject doc = null;
	
	/**
	  "updates": {
		"ADD_PERSON":{
	    "sparql":"INSERT DATA { ?person rdf:type iot:Person . ?person iot:hasName ?name }",
	    "forcedBindings": {
		"person" : {"type":"uri", "value":""},
		"name" : {"type":"literal", "value":""}}}
    }, 
	 */
	public String update(String updateID) {
		JsonElement elem = null;
		if ((elem = doc.get("updates")) != null) 
			if ((elem = elem.getAsJsonObject().get(updateID))!=null)
				if ((elem = elem.getAsJsonObject().get("sparql"))!=null) return elem.getAsString();
		return null;
	}
	
	public String subscribe(String subscribeID) {
		JsonElement elem = null;
		if ((elem = doc.get("subscribes")) != null) 
			if ((elem = elem.getAsJsonObject().get(subscribeID))!=null)
				if ((elem = elem.getAsJsonObject().get("sparql"))!=null) return elem.getAsString();
		return null;
	}
	
	public Set<String> getUpdateIds() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if((elem = doc.get("updates"))!=null)
			for (Entry<String, JsonElement> key :elem.getAsJsonObject().entrySet()){
				ret.add(key.getKey());
			}
		return ret;
	}
	
	public Set<String> getSubscribeIds() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if((elem = doc.get("subscribes"))!=null)
			for (Entry<String, JsonElement> key :elem.getAsJsonObject().entrySet()){
				ret.add(key.getKey());
			}
		return ret;
	}
	
	/**
	 * "forcedBindings": {
		"person" : {"type":"uri", "value":""},
		"name" : {"type":"literal", "value":""}}}
		
	 * @param selectedValue
	 * @return
	 */
	public Bindings updateBindings(String selectedValue) {
		JsonElement elem;
		Bindings ret = new Bindings();
		if ((elem = doc.get("updates")) !=null)
			if((elem = elem.getAsJsonObject().get(selectedValue))!=null)
				if((elem = elem.getAsJsonObject().get("forcedBindings"))!=null) {
					for (Entry<String, JsonElement> binding : elem.getAsJsonObject().entrySet()) {
						JsonObject value = binding.getValue().getAsJsonObject();
						RDFTerm bindingValue = null;
				
						if (value.get("type")!=null){
							if(value.get("type").getAsString().equals("uri")) {
								bindingValue = new RDFTermURI(value.get("value").getAsString());
							}
							else {
								bindingValue = new RDFTermLiteral(value.get("value").getAsString());
							}
						}
						ret.addBinding(binding.getKey(), bindingValue);
					}
				}
		return ret;
	}
	
	public Bindings subscribeBindings(String selectedValue) {
		JsonElement elem;
		Bindings ret = new Bindings();
		if ((elem = doc.get("subscribes")) !=null)
			if((elem = elem.getAsJsonObject().get(selectedValue))!=null)
				if((elem = elem.getAsJsonObject().get("forcedBindings"))!=null) {
					for (Entry<String, JsonElement> binding : elem.getAsJsonObject().entrySet()) {
						JsonObject value = binding.getValue().getAsJsonObject();
						RDFTerm bindingValue = null;
				
						if (value.get("type")!=null){
							if(value.get("type").getAsString().equals("uri")) {
								bindingValue = new RDFTermURI(value.get("value").getAsString());
							}
							else {
								bindingValue = new RDFTermLiteral(value.get("value").getAsString());
							}
						}
						ret.addBinding(binding.getKey(), bindingValue);
					}
				}
		return ret;
	}
	
	/**
	"parameters":{
	  	"ports":{"ws":9000,"wss":9443,"http":8000,"https":8443},
		"paths":{"http":"/sparql","https":"/sparql","ws":"/sparql","wss":"/secure/sparql","register":"/oauth/register","token":"/oauth/token"},
	    "host": "localhost"
	   }
	*/
	
	public String getHost() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("host"))!=null) return elem.getAsString();
		return null;
	}

	public int getPort() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("ports"))!=null) 
				if ((elem = elem.getAsJsonObject().get("http"))!=null) return elem.getAsInt();
		return -1;
	}
	
	public int getSecurePort() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("ports"))!=null) 
				if ((elem = elem.getAsJsonObject().get("https"))!=null) return elem.getAsInt();
		return -1;
	}

	public int getSubscribePort() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("ports"))!=null) 
				if ((elem = elem.getAsJsonObject().get("ws"))!=null) return elem.getAsInt();
		return -1;
	}
	
	public int getSecureSubscribePort() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("ports"))!=null) 
				if ((elem = elem.getAsJsonObject().get("wss"))!=null) return elem.getAsInt();
		return -1;
	}

	public String getPath() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("paths"))!=null) 
				if ((elem = elem.getAsJsonObject().get("http"))!=null) return elem.getAsString();
		return null;
	}
	
	public String getSecurePath() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("paths"))!=null) 
				if ((elem = elem.getAsJsonObject().get("https"))!=null) return elem.getAsString();
		return null;
	}
	
	public String getSecureSubscribePath() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("paths"))!=null) 
				if ((elem = elem.getAsJsonObject().get("wss"))!=null) return elem.getAsString();
		return null;
	}
	
	public String getSubscribePath() {
		JsonElement elem = null;
		if ((elem = doc.get("parameters")) != null) 
			if ((elem = elem.getAsJsonObject().get("paths"))!=null) 
				if ((elem = elem.getAsJsonObject().get("ws"))!=null) return elem.getAsString();
		return null;
	}

	/**
	 * "namespaces" : { "iot":"http://www.arces.unibo.it/iot#","rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#"},
	 */
	     	
	public Set<String> getPrefixes() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if((elem = doc.get("namespaces"))!=null)
			for (Entry<String, JsonElement> key :elem.getAsJsonObject().entrySet()){
				ret.add(key.getKey());
			}
		return ret;
	}
	
	public String getNamespaceURI(String prefix) {
		JsonElement elem;
		String ret = null;
		if((elem = doc.get("namespaces"))!=null)
			if((elem = elem.getAsJsonObject().get(prefix))!= null)
				return elem.getAsString();
		return ret;
	}
	
	public synchronized boolean load(String fileName){
		
		loaded = false;
		
		logger.debug("Loading: "+fileName);
		
		try {
			File inputFile = new File(fileName);
			Reader reader = new FileReader(inputFile);
			doc = new JsonParser().parse(reader).getAsJsonObject();
			if (doc == null) {
				logger.error("Failed to parse "+fileName);
				return false;
			}
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		loaded = true;
				
		return true;
	}

	public boolean isLoaded() {
		return loaded;
	}

	
}
