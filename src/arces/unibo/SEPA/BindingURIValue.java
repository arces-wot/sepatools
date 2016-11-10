package arces.unibo.SEPA;

import java.util.HashMap;

public class BindingURIValue extends BindingValue {

	public BindingURIValue(String value,HashMap<String,String> URI2PrefixMap,boolean added){
		this.added = added;
		this.type = BINDING_TYPE.URI;
		if (URI2PrefixMap != null){
			for(String uri : URI2PrefixMap.keySet()){
				if (value.contains(uri)) {
					this.value = value.replace(uri, URI2PrefixMap.get(uri)+":");
					return;
				}
			}
		}
		this.value = value;	
	}
	
	public BindingURIValue(String value){
		this.type = BINDING_TYPE.URI;
		this.value = value;
		this.added = true;
	}
	
	public BindingURIValue(String value,boolean added){
		this.type = BINDING_TYPE.URI;
		this.value = value;
		this.added = added;
	}
	
	public String getValue() {
		return value;
	}
}
