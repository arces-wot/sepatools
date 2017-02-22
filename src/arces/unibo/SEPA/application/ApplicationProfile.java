/* This class implements a DOM parser of an .sap file instance of the ApplicationProfile.xsd schema
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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import arces.unibo.SEPA.application.SEPALogger.VERBOSITY;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.RDFTermLiteral;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;

public class ApplicationProfile {	
	private final String tag ="SPARQL PARSER";
	
	private HashMap<String,String> updateMap = new HashMap<>();
	private HashMap<String,String> subscribeMap = new HashMap<>();
	
	private HashMap<String,Bindings> updateBindingsMap = new HashMap<>();
	private HashMap<String,Bindings> subscribeBindingsMap = new HashMap<>();
	
	private HashMap<String,String> namespaceMap = new HashMap<>();
	
	private Parameters params = new Parameters();
	
	private Document doc = null;
	private boolean loaded = false;
	
	public class Parameters {
		private String url = "mml.arces.unibo.it";
		private int updatePort = 8000;
		private int subscribePort = 9000;
		private String path = "/sparql";
		
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
		if (ret == null) SEPALogger.log(VERBOSITY.ERROR, tag, "Prefix " + prefix + " NOT FOUND");
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
			SEPALogger.log(VERBOSITY.ERROR, tag, "SUBSCRIBE ID <" + id + "> NOT FOUND");
			return null;
		}
		return subscribeMap.get(id);
	}
	
	public String update(String id) {
		if (!updateMap.containsKey(id)) {
			SEPALogger.log(VERBOSITY.ERROR, tag, "UPDATE ID <" + id + "> NOT FOUND");
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
		
		SAXBuilder builder = new SAXBuilder();
		File inputFile = new File(fileName);
		
		try {
			doc = builder.build(inputFile);
		} catch (JDOMException | IOException e) {
			SEPALogger.log(VERBOSITY.FATAL, tag, e.getMessage());
			return false;
		}
		
		Element root = doc.getRootElement();
		
		if (root == null) return false;
		
		List<Element> nodes = root.getChildren();
		
		for (Element node : nodes) {
			HashMap<String,String> sparqlMap = null;
			HashMap<String,Bindings> bindingsMap = null;
			
			switch(node.getName()){
				case "subscribes":
					sparqlMap = subscribeMap;
					bindingsMap = subscribeBindingsMap;
					break;
				case "updates":
					sparqlMap = updateMap;
					bindingsMap = updateBindingsMap;
					break;
				case "namespaces":
					sparqlMap = namespaceMap;
					break;
				case "parameters":
					List<Attribute> attributes = node.getAttributes();
					for (Attribute attr : attributes) {
						switch(attr.getName()){
							case "updateport":
								try {
									params.setUpdatePort(attr.getIntValue());
								} catch (DataConversionException e) {
									e.printStackTrace();
									SEPALogger.log(VERBOSITY.ERROR, tag, "Error parsing application profile \"updateport\" parameter (found: " + attr.getValue() + ")");
								}
								break;
							case "subscribeport":
								try {
									params.setSubscribePort(attr.getIntValue());
								} catch (DataConversionException e) {
									e.printStackTrace();
									SEPALogger.log(VERBOSITY.ERROR, tag, "Error parsing application profile \"subscribeport\" parameter (found: " + attr.getValue() + ")");
								}
								break;
							case "url":
								params.setUrl(attr.getValue());
								break;
							case "path":
								params.setPath(attr.getValue());
								break;
						}
					}
					break;
			}
			
			List<Element> elements = node.getChildren();
			
			if (elements == null) continue;
			
			for(Element element : elements) {
				if (node.getName().equals("namespaces")) {
					sparqlMap.put(element.getAttributeValue("prefix"), element.getAttributeValue("suffix"));
					continue;
				}
				
				sparqlMap.put(element.getAttributeValue("id"), element.getChildText("sparql"));

				Element forcedBindings = element.getChild("forcedBindings");				
				if (forcedBindings == null) continue;
				
				List<Element> bindingElements = forcedBindings.getChildren();
				Bindings bindings = new Bindings();
				for (Element bindingElement : bindingElements) {
					if (bindingElement.getAttributeValue("type").equals("uri"))
						bindings.addBinding(bindingElement.getAttributeValue("variable"), new RDFTermURI(bindingElement.getAttributeValue("value")));
					else
						bindings.addBinding(bindingElement.getAttributeValue("variable"), new RDFTermLiteral(bindingElement.getAttributeValue("value")));
				}
				
				bindingsMap.put(element.getAttributeValue("id"), bindings);
			}
		}
		
		loaded = true;
		
		return true;
	}
	public boolean isLoaded() {
		return loaded;
	}
}
