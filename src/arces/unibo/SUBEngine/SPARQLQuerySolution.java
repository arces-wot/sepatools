package arces.unibo.SUBEngine;

import java.util.HashMap;
import java.util.Set;

/**
 * This class represents a single query solution of a SPARQL 1.1 Query
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class SPARQLQuerySolution {
	private HashMap<String,RDFTerm> bindingsMap;

	public Set<String> getVariables() {
		return bindingsMap.keySet();
	}
	
	public RDFTerm getBindingValue(String variable){
		return bindingsMap.get(variable);
	}
	
	public void addBinding(String variable,RDFTerm value){
		bindingsMap.put(variable, value);
	}
	
	public abstract class RDFTerm {
		private String value = null;
		
		public String getValue() {
			return value;
		}
	}
	
	public class RDFTermURI extends RDFTerm {
		
		public RDFTermURI(String value) {
			super.value = value;
		}
	}
	
	public class RDFTermBNode extends RDFTerm {
		
		public RDFTermBNode(String value) {
			super.value = value;
		}
	}

	public class RDFTermLiteral extends RDFTerm {
		private String languageTag = null;
		private String datatype = null;
		
		public RDFTermLiteral(String value) {
			super.value = value;
		}
		
		public RDFTermLiteral(String value,String lanOrDT,boolean lan) {
			super.value = value;
			if (lan) languageTag = lanOrDT;
			else datatype = lanOrDT;
		}
		
		public String getLanguageTag(){
			return languageTag;
		}
		
		public String getDatatype() {
			return datatype;
		}
	}
}
