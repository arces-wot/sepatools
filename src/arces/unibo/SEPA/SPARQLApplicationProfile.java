package arces.unibo.SEPA;

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

import arces.unibo.SEPA.Logger.VERBOSITY;

public class SPARQLApplicationProfile {	
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
		private String url = "127.0.0.1";
		private int port = 10010;
		private String name = "IoT";
		
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
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
		if (ret == null) Logger.log(VERBOSITY.ERROR, tag, "Prefix " + prefix + " NOT FOUND");
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
			Logger.log(VERBOSITY.ERROR, tag, "SUBSCRIBE ID <" + id + "> NOT FOUND");
			return null;
		}
		return subscribeMap.get(id);
	}
	
	public String update(String id) {
		if (!updateMap.containsKey(id)) {
			Logger.log(VERBOSITY.ERROR, tag, "UPDATE ID <" + id + "> NOT FOUND");
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
			Logger.log(VERBOSITY.FATAL, tag, e.getMessage());
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
							case "port":
							try {
								params.setPort(attr.getIntValue());
							} catch (DataConversionException e) {
								e.printStackTrace();
								Logger.log(VERBOSITY.ERROR, tag, "Error parsing application profile \"port\" parameter (found: " + attr.getValue() + ")");
							}
								break;
							case "url":
								params.setUrl(attr.getValue());
								break;
							case "name":
								params.setName(attr.getValue());
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
						bindings.addBinding(bindingElement.getAttributeValue("variable"), new BindingURIValue(bindingElement.getAttributeValue("value")));
					else
						bindings.addBinding(bindingElement.getAttributeValue("variable"), new BindingLiteralValue(bindingElement.getAttributeValue("value")));
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
