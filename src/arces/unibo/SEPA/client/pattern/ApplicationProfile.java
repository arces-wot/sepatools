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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import arces.unibo.SEPA.client.api.SPARQL11SEProperties;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.RDFTerm;
import arces.unibo.SEPA.commons.SPARQL.RDFTermLiteral;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;

/** SAP file example
 {
    "parameters":{},
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
public class ApplicationProfile extends SPARQL11SEProperties {	
	public ApplicationProfile(String propertiesFile) throws FileNotFoundException, NoSuchElementException, IOException {
		super(propertiesFile);
	}

	public ApplicationProfile(String propertiesFile,byte[] secret) throws FileNotFoundException, NoSuchElementException, IOException {
		super(propertiesFile,secret);	
	}
	
	protected Logger logger = LogManager.getLogger("SAP");	

	private String fileName;
	
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

	public String getFileName() {
		return fileName;
	}

	public String printParameters() {
		return parameters.toString();
	}
}
