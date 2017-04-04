/* This class implements the OAuth 2.0 Authorization Manager (AM) of the SEPA
    Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.security;

import java.io.FileInputStream;
import java.io.IOException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;

import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

public class AuthorizationManager {
	
	//IDENTITY ==> ID
	private HashMap<String,String> clients = new HashMap<String,String>();
	
	//ID ==> Secret
	private HashMap<String,String> credentials = new HashMap<String,String>();
	
	//ID ==> JWTClaimsSet
	private HashMap<String,JWTClaimsSet> clientClaims = new HashMap<String,JWTClaimsSet>();
	
	//*************************
	//JWT signing and verifying
	//*************************
	private JWSSigner signer;
	private RSASSAVerifier verifier;
	private JsonElement jwkPublicKey;
	private ConfigurableJWTProcessor<SEPASecurityContext> jwtProcessor;
	private SEPASecurityContext context = new SEPASecurityContext();

	private long expiring = 60; 												//TODO: JMX
	private String issuer = "https://wot.arces.unibo.it:8443/oauth/token"; 		//TODO: JMX
	private String httpsAudience = "https://wot.arces.unibo.it:8443/sparql"; 	//TODO: JMX
	private String wssAudience ="wss://wot.arces.unibo.it:9443/sparql";  		//TODO: JMX
	 
	private static final Logger logger = LogManager.getLogger("Authorization manager");
	
	/*
	Security context. Provides additional information necessary for processing a JOSE object.
	Example context information:

	Identifier of the message producer (e.g. OpenID Connect issuer) to retrieve its public key to verify the JWS signature.
	Indicator whether the message was received over a secure channel (e.g. TLS/SSL) which is essential for processing unsecured (plain) JOSE objects.
	*/
	private class SEPASecurityContext implements SecurityContext {
		
	}
	
	public void securityCheck(String identity) {
		logger.debug("*********Security check START***********");
		//Register
		logger.debug("Register: "+identity);
		JsonObject json = register(identity);
		String id = json.get("client_id").getAsString();
		String secret = json.get("client_secret").getAsString();
		String auth = id+":"+secret;	
		logger.debug("ID:SECRET="+auth);
		
		//Get token
		String encodedCredentials = Base64.getEncoder().encodeToString(auth.getBytes());
		logger.debug("Authorization Basic "+encodedCredentials);
		json = getToken(encodedCredentials);
		String access_token = json.get("access_token").getAsString();
		logger.debug("Access token: "+access_token);
		
		//Validate token
		if(validateToken(access_token)) logger.debug("VALIDATED :-)");
		else logger.debug("FAILED :-(");
		logger.debug("*********Security check END***********");
	}

	private boolean loadCertificate(String keyStorePath,String keystorePwd,String keyPwd,String keyID) {
		// Specify the key store type, e.g. JKS
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("JKS");
		} catch (KeyStoreException e) {
			logger.error(e.getMessage());
			return false;
		}

		// Load the key store from file
		try {
			keyStore.load(new FileInputStream(keyStorePath), keystorePwd.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		// Load the key from the key store
		RSAKey jwk;
		try {
			jwk = RSAKey.load(keyStore, "SepaKey", keyPwd.toCharArray());
		} catch (KeyStoreException | JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		//Get the private and public keys to sign and verify
		RSAPrivateKey privateKey;
		RSAPublicKey publicKey;
		try {
			privateKey = jwk.toRSAPrivateKey();
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		try {
			publicKey = jwk.toRSAPublicKey();
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		// Create RSA-signer with the private key
		signer = new RSASSASigner(privateKey);
		
		// Create RSA-verifier with the public key
		verifier = new RSASSAVerifier(publicKey);
				
		//Serialize the public key to be deliverer during registration
		jwkPublicKey = new JsonParser().parse(jwk.toPublicJWK().toJSONString());
		
		// Set up a JWT processor to parse the tokens and then check their signature
		// and validity time window (bounded by the "iat", "nbf" and "exp" claims)
		jwtProcessor = new DefaultJWTProcessor<SEPASecurityContext>();
		JWKSet jws = new JWKSet(jwk);
		JWKSource<SEPASecurityContext> keySource = new ImmutableJWKSet<SEPASecurityContext>(jws);
		JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
		JWSKeySelector<SEPASecurityContext> keySelector = new JWSVerificationKeySelector<SEPASecurityContext>(expectedJWSAlg, keySource);
		jwtProcessor.setJWSKeySelector(keySelector);
		
		return true;
	}
	
	public AuthorizationManager() {		
		loadCertificate("sepa.jks","SepaKeystore2017","SepaKey2017","SepaKey");
		
		securityCheck(UUID.randomUUID().toString());
	}
	
	private boolean authorizeIdentity(String id) {
		//TODO: check if "name" is registered in the users DB
		return true;
	}
	/*
	 * POST https://wot.arces.unibo.it:8443/oauth/token
	 * 
	 * Accept: application/json
	 * Content-Type: application/json
	 * 
	 * { 
	 * "client_identity": ”<ClientIdentity>", 
	 * "grant_types": ["client_credentials"] 
	 * }
	 * 
	 * Response example:
	 * { 	"client_id": "889d02cf-16dd-4934-9341-a754088faxyz",
	 * 		"client_secret": "ahd5MU42J0hIxPXzhUhjJHt2d0Oc5M6B644CtuwUlE9zpSuF14-kXYZ",
	 * 		"signature" : JWK RSA public key (can be used to verify the signature),
	 * 		"authorized" : Boolean
	 * }
	 * */
	public JsonObject register(String identity) {
		JsonObject response = new JsonObject();	
		
		//Check if entity is authorized to request credentials
		if (!authorizeIdentity(identity)) {
			logger.warn("Not authorized indentity "+identity);
			response.add("client_id", new JsonPrimitive("Not authorized identity"));
			response.add("client_secret", new JsonPrimitive("Not authorized identity"));
			response.add("signature", new JsonPrimitive("Not authorized identity"));
			response.add("authorized", new JsonPrimitive(false));
			return response;
		}
		
		//Multiple registration not allowed
		if (clients.containsKey(identity)) {
			logger.warn("Multiple registration forbitten "+identity);
			response.add("client_id", new JsonPrimitive("Multiple registration forbitten"));
			response.add("client_secret", new JsonPrimitive("Multiple registration forbitten"));
			response.add("signature", new JsonPrimitive("Multiple registration forbitten"));
			response.add("authorized", new JsonPrimitive(false));
			return response;
		}
		
		//Create credentials
		String client_id = UUID.randomUUID().toString();
		String client_secret = UUID.randomUUID().toString();
		
		//Store credentials
		while(credentials.containsKey(client_id)) client_id = UUID.randomUUID().toString();
		credentials.put(client_id,client_secret);
		
		//Register client
		clients.put(identity, client_id);
		
		//Response
		response.add("client_id", new JsonPrimitive(client_id));
		response.add("client_secret", new JsonPrimitive(client_secret));
		response.add("signature", jwkPublicKey);
		response.add("authorized", new JsonPrimitive(true));
		
		return response;
	}
	
	/*
	 * POST https://wot.arces.unibo.it:8443/oauth/token
	 * 
	 * Content-Type: application/x-www-form-urlencoded
	 * Accept: application/json
	 * Authorization: Basic Basic64(id:secret)
	 * 
	 * Response example:
	 * { 	"access_token": "eyJraWQiOiIyN.........",
	 * 		"token_type": "bearer",
	 * 		"expires_in": 3600 
	 * }
	 * */
	public JsonObject getToken(String encodedCredentials) {	
		//Produce JWT compliant with WoT W3C recommendations
		JsonObject jwt = new JsonObject();
				
		//Decode credentials
		byte[] decoded = null;
		try{
			decoded = Base64.getDecoder().decode(encodedCredentials);
		}
		catch (IllegalArgumentException e) {
			logger.error("Not authorized");
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Not authorized"));
			return jwt;
		}
		String decodedCredentials = new String(decoded);
		String[] clientID = decodedCredentials.split(":");
		if (clientID==null){
			logger.error("Wrong Basic authorization");
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Not authorized"));
			return jwt;	
		}
		if (clientID.length != 2) {
			logger.error("Wrong Basic authorization");
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Not authorized"));
			return jwt;	
		}
		
		String id = decodedCredentials.split(":")[0];
		String secret = decodedCredentials.split(":")[1];
		logger.debug("Credentials: "+id+" "+secret);
		
		//Verify credentials
		if (!credentials.containsKey(id)) {
			logger.error("Client id: "+id+" is not registered");
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Wrong credentials"));
			return jwt;
		}
		
		if (!credentials.get(id).equals(secret)) {
			logger.error("Wrong secret: "+secret+ " for client id: "+id);
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Wrong credentials"));
			return jwt;
		}
		
		//Check is a token has been release for this client
		if (clientClaims.containsKey(id)) {
			//Do not return a new token if the previous one is not expired
			Date expires = clientClaims.get(id).getExpirationTime();
			Date now = new Date();
			logger.debug("Check token expiration: "+now+" > "+expires+ " ?");
			if(now.before(expires)) {
				logger.error("Token is not expired");
				jwt.add("access_token", new JsonPrimitive(""));
				jwt.add("token_type", new JsonPrimitive(""));
				jwt.add("expires_in", new JsonPrimitive(""));		
				jwt.add("authorized", new JsonPrimitive(false));
				jwt.add("reason", new JsonPrimitive("Your token is not expired. You can not request a new token"));
				return jwt;	
			}
		}
		
		// Prepare JWT with claims set
		 JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
		 long timestamp = new Date().getTime();
		 
		/*
		 * 4.1.1.  "iss" (Issuer) Claim

	   The "iss" (issuer) claim identifies the principal that issued the
	   JWT.  The processing of this claim is generally application specific.
	   The "iss" value is a case-sensitive string containing a StringOrURI
	   value.  Use of this claim is OPTIONAL.*/
		 
		 claimsSetBuilder.issuer(issuer);
		 
	 /* 4.1.2.  "sub" (Subject) Claim

	   The "sub" (subject) claim identifies the principal that is the
	   subject of the JWT.  The Claims in a JWT are normally statements
	   about the subject.  The subject value MUST either be scoped to be
	   locally unique in the context of the issuer or be globally unique.
	   The processing of this claim is generally application specific.  The
	   "sub" value is a case-sensitive string containing a StringOrURI
	   value.  Use of this claim is OPTIONAL.*/
		 
		 claimsSetBuilder.subject("SEPADemoApp");
		
	 /* 4.1.3.  "aud" (Audience) Claim

	   The "aud" (audience) claim identifies the recipients that the JWT is
	   intended for.  Each principal intended to process the JWT MUST
	   identify itself with a value in the audience claim.  If the principal
	   processing the claim does not identify itself with a value in the
	   "aud" claim when this claim is present, then the JWT MUST be
	   rejected.  In the general case, the "aud" value is an array of case-
	   sensitive strings, each containing a StringOrURI value.  In the
	   special case when the JWT has one audience, the "aud" value MAY be a
	   single case-sensitive string containing a StringOrURI value.  The
	   interpretation of audience values is generally application specific.
	   Use of this claim is OPTIONAL.*/
		 
		 ArrayList<String> audience = new ArrayList<String>();
		 audience.add(httpsAudience);
		 audience.add(wssAudience);
		 claimsSetBuilder.audience(audience);
		
		/* 4.1.4.  "exp" (Expiration Time) Claim

	   The "exp" (expiration time) claim identifies the expiration time on
	   or after which the JWT MUST NOT be accepted for processing.  The
	   processing of the "exp" claim requires that the current date/time
	   MUST be before the expiration date/time listed in the "exp" claim.
	   Implementers MAY provide for some small leeway, usually no more than
	   a few minutes, to account for clock skew.  Its value MUST be a number
	   containing a NumericDate value.  Use of this claim is OPTIONAL.*/
		
		 claimsSetBuilder.expirationTime(new Date(timestamp+(expiring*1000)));
		
		/*4.1.5.  "nbf" (Not Before) Claim

	   The "nbf" (not before) claim identifies the time before which the JWT
	   MUST NOT be accepted for processing.  The processing of the "nbf"
	   claim requires that the current date/time MUST be after or equal to
	   the not-before date/time listed in the "nbf" claim.  Implementers MAY
	   provide for some small leeway, usually no more than a few minutes, to
	   account for clock skew.  Its value MUST be a number containing a
	   NumericDate value.  Use of this claim is OPTIONAL.*/
		
		 claimsSetBuilder.notBeforeTime(new Date(timestamp-1000));
		
		/* 4.1.6.  "iat" (Issued At) Claim

	   The "iat" (issued at) claim identifies the time at which the JWT was
	   issued.  This claim can be used to determine the age of the JWT.  Its
	   value MUST be a number containing a NumericDate value.  Use of this
	   claim is OPTIONAL.*/

		claimsSetBuilder.issueTime(new Date(timestamp));
		
		/*4.1.7.  "jti" (JWT ID) Claim

	   The "jti" (JWT ID) claim provides a unique identifier for the JWT.
	   The identifier value MUST be assigned in a manner that ensures that
	   there is a negligible probability that the same value will be
	   accidentally assigned to a different data object; if the application
	   uses multiple issuers, collisions MUST be prevented among values
	   produced by different issuers as well.  The "jti" claim can be used
	   to prevent the JWT from being replayed.  The "jti" value is a case-
	   sensitive string.  Use of this claim is OPTIONAL.*/
		
		claimsSetBuilder.jwtID(id+":"+secret);

		JWTClaimsSet jwtClaims = claimsSetBuilder.build();
		
		//******************************
		// Sign JWT with private RSA key
		//******************************
		SignedJWT signedJWT;
		try {
			signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), JWTClaimsSet.parse(jwtClaims.toString()));
		} catch (ParseException e) {
			logger.error(e.getMessage());
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Internal error on signing token (1)"));
			return jwt;
		}
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			jwt.add("access_token", new JsonPrimitive(""));
			jwt.add("token_type", new JsonPrimitive(""));
			jwt.add("expires_in", new JsonPrimitive(""));		
			jwt.add("authorized", new JsonPrimitive(false));
			jwt.add("reason", new JsonPrimitive("Internal error on signing token (2)"));
			return jwt;
		}
				
		jwt.add("access_token", new JsonPrimitive(signedJWT.serialize()));
		jwt.add("token_type", new JsonPrimitive("bearer"));
		jwt.add("expires_in", new JsonPrimitive(expiring));		
		jwt.add("authorized", new JsonPrimitive(true));
		jwt.add("reason", new JsonPrimitive(""));
		
		//Add the token to the released tokens
		clientClaims.put(id, jwtClaims);
		
		return jwt;
	}
	
	public boolean validateToken(String accessToken) {
		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(accessToken);
		} catch (ParseException e) {
			logger.error(e.getMessage());
			return false;
		}

		try {
			 if(!signedJWT.verify(verifier)) return false;
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		// Process the token
		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(accessToken, context);
		} catch (ParseException | BadJOSEException | JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		Date now = new Date();
		if (now.after(claimsSet.getExpirationTime())) {
			logger.debug("Token is expired "+claimsSet.getExpirationTime());
			return false;
		}
		if (now.before(claimsSet.getNotBeforeTime())) {
			logger.debug("Token can not be used before: "+claimsSet.getNotBeforeTime());
			return false;
		}
		if (!claimsSet.getIssuer().equals(issuer)) {
			logger.debug("Issuer not recognized");
			return false;
		}
		String[] id = claimsSet.getJWTID().split(":");
		if (id == null) {
			logger.debug("JWT ID not recognized (1)");
			return false;
		}
		if (id.length != 2) {
			logger.debug("JWT ID not recognized (2)");
			return false;	
		}
		if (!credentials.containsKey(id[0])) {
			logger.debug("JWT ID not recognized (3)");
			return false;
		}
		if (!credentials.get(id[0]).equals(id[1])) {
			logger.debug("JWT ID not recognized (4)");
			return false;	
		}
				
		logger.debug(claimsSet.toJSONObject());
		
		return true;
	}	
}
