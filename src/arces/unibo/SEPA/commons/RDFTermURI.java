package arces.unibo.SEPA.commons;

import com.google.gson.JsonPrimitive;

public class RDFTermURI extends RDFTerm {
	
	public RDFTermURI(String value) {
		super(value);
		json.add("type", new JsonPrimitive("uri"));
	}	
}