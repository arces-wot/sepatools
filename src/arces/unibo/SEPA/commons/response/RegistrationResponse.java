package arces.unibo.SEPA.commons.response;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** 
 {"client_id":"5b60a155-bada-4499-bc6f-26b4d37bc1ef",
 "client_secret":"40e18d77-421c-48ce-a44a-14da1238e923",
 "signature":
	{"kty":"RSA",
	"e":"AQAB",
	"x5t":"...",
	"kid":"sepacertificate",
	"x5c":["..."],
	"n":"..."}}
   * 
   * @author Luca Roffia
   * 
*/
public class RegistrationResponse extends Response {
	public RegistrationResponse(String client_id,String client_secret,JsonElement jwkPublicKey) {
		super();
		json.add("client_id", new JsonPrimitive(client_id));
		json.add("client_secret", new JsonPrimitive(client_secret));
		json.add("signature", jwkPublicKey);
	}
	
	public String getClientId(){
		return json.get("client_id").getAsString();
	}
	
	public String getClientSecret(){
		return json.get("client_secret").getAsString();
	}
}
