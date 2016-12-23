package arces.unibo.SEPA.commons;

import com.google.gson.JsonPrimitive;

public class RDFTermBNode extends RDFTerm {
	
	public RDFTermBNode(String value) {
		super(value);
		
		json.add("type", new JsonPrimitive("bnode"));
	}
}

