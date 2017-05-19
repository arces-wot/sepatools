package arces.unibo.SEPA.client.tools;

import java.util.HashMap;

import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import arces.unibo.SEPA.client.pattern.ApplicationProfile;
import arces.unibo.SEPA.client.pattern.Consumer;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.response.ErrorResponse;

public class Explorer {
	ApplicationProfile appProfile = new ApplicationProfile();
	private DefaultTableModel propertiesDM;
	
	class PropertyMonitor extends Consumer {
		OWLClassNodeModel root;
		OWLClassNodeModel domain; 
		OWLClassNodeModel range;
		
		public PropertyMonitor(ApplicationProfile appProfile, String subscribeID) {
			super(appProfile, subscribeID);
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {
			
		}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
			for (Bindings binding : bindingsResults.getBindings()) {
				String propertyURI = "";
				String domainURI = "";
				String rangeURI = "";
				String comment = "";
				
				if (binding.getBindingValue("property") != null) propertyURI =(binding.getBindingValue("property"));
				if (binding.getBindingValue("domain") != null) domainURI = (binding.getBindingValue("domain"));	
				if (binding.getBindingValue("range") != null) rangeURI = (binding.getBindingValue("range"));
				if (binding.getBindingValue("comment") != null) comment = binding.getBindingValue("comment");
				
				if (propertyURI.equals("")) continue; 
				
				propertiesDM.addRow(new String[]{propertyURI,domainURI,rangeURI,comment});
			}
			
		}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {
			
			
		}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {
			propertiesDM.getDataVector().clear();
			
			for (Bindings binding : bindingsResults.getBindings()) {
				String propertyURI = "";
				String domainURI = "";
				String rangeURI = "";
				String comment = "";
				
				if (binding.getBindingValue("property") != null) propertyURI = (binding.getBindingValue("property"));
				if (binding.getBindingValue("domain") != null) domainURI = (binding.getBindingValue("domain"));	
				if (binding.getBindingValue("range") != null) rangeURI = (binding.getBindingValue("range"));
				if (binding.getBindingValue("comment") != null) comment = binding.getBindingValue("comment");
				
				if (propertyURI.equals("")) continue; 
				
				propertiesDM.addRow(new String[]{propertyURI,domainURI,rangeURI,comment});
			}
		}

		@Override
		public void brokenSubscription() {
			
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			
			
		}
		
	}
	
	class ClassMonitor extends Consumer {
		private HashMap<String,OWLClassNodeModel> treeMap = new HashMap<String,OWLClassNodeModel>();
		OWLClassNodeModel root;
		
		public ClassMonitor(ApplicationProfile appProfile, String subscribeID) {
			super(appProfile, subscribeID);
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {
			for (Bindings binding : bindingsResults.getBindings()) {
				String classURI = null;
				String classLabel = null;
				String classComment = null;
				String subclassURI = null;
				
				if (binding.getBindingValue("class") != null) classURI = (binding.getBindingValue("class"));
				if (binding.getBindingValue("subclass") != null) subclassURI = (binding.getBindingValue("subclass"));	
				if (binding.getBindingValue("label") != null) classLabel = binding.getBindingValue("label");
				if (binding.getBindingValue("comment") != null) classComment = binding.getBindingValue("comment");
				
				OWLClassNodeModel classNode = null;
				OWLClassNodeModel subclassNode = null;
				
				if (classURI != null) {
					if (!treeMap.containsKey(classURI)) {
						classNode = new OWLClassNodeModel(classURI);
						root.add(classNode);
						treeMap.put(classURI, classNode);
					}
					else {
						classNode = treeMap.get(classURI);
					}
					
					//Label & comment
					if (classLabel != null) {
						classNode.setLabel(classLabel);
						if (classComment != null) classNode.setComment("URI: "+classURI+"\n\n"+classComment);
						else classNode.setComment("URI: "+classURI);
					}
					else if (classComment != null) classNode.setComment(classComment);
				}
				
				if (subclassURI != null) {
					if (!treeMap.containsKey(subclassURI)){
						subclassNode = new OWLClassNodeModel(subclassURI);
						treeMap.put(subclassURI, subclassNode);
					}
					else subclassNode = treeMap.get(subclassURI);
						
					classNode.add(subclassNode);	
				}
			}
			
		}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {
			
		}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {
			root = new OWLClassNodeModel("owl:Thing");
			
			treeMap.clear();
			
			notifyAdded(bindingsResults,spuid,0);
		}

		@Override
		public void brokenSubscription() {
			
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			
			
		}
		
	}
	
	static class OWLClassNodeModel extends DefaultMutableTreeNode {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6299628084428311514L;

		private String label = null;
		private String uri = null;
		private String comment = null;
		
		public OWLClassNodeModel(String uri) {
			super(uri);
			this.uri = uri;
		}
		
		@Override
		public String toString() {
			if (label != null) return label;
			return uri;
		}
		
		public String getComment() {
			if (comment == null) return "";
			return comment;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
		
		public void setComment(String comment) {
			this.comment = comment;
		}
		
	}
}
