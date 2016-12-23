package arces.unibo.SEPA.commons;

import com.google.gson.JsonPrimitive;

public class RDFTermLiteral extends RDFTerm {
	
	public RDFTermLiteral(String value) {
		super(value);
		
		json.add("type", new JsonPrimitive("literal"));
	}
	
	public RDFTermLiteral(String value,String lanOrDT,boolean lan) {
		super(value);
		
		json.add("type", new JsonPrimitive("literal"));
		
		if (lan) json.add("xml:lang", new JsonPrimitive(lanOrDT)); 
		else json.add("datatype", new JsonPrimitive(lanOrDT)); 
	}
	
	public String getLanguageTag(){
		return json.get("xml:lang").getAsString();
	}
	
	public String getDatatype() {
		return json.get("datatype").getAsString();
	}
}
