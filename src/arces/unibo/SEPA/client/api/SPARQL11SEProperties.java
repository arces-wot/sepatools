/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

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

package arces.unibo.SEPA.client.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.*;

import java.util.Date;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.commons.protocol.SPARQL11Properties;

import sun.misc.*;

/**
 * The Class SPARQL11SEProperties.
 */
public class SPARQL11SEProperties extends SPARQL11Properties {
	
	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProperties");
	/**
	 * The new primitives introduced by the SPARQL 1.1 SE Protocol are: 
	 * 
	 * SECUREUPDATE,SECUREQUERY,SUBSCRIBE,SECURESUBSCRIBE,UNSUBSCRIBE,SECUREUNSUBSCRIBE,REGISTER,REQUESTTOKEN
	 * 
	 * 
	* @author Luca Roffia (luca.roffia@unibo.it)
	* @version 0.1
	* */
	public enum SPARQL11SEPrimitive {
		/** A secure update primitive */
		SECUREUPDATE,
		/** A subscribe primitive */
		SUBSCRIBE,
		/** A secure subscribe primitive. */
		SECURESUBSCRIBE,
		/** A unsubscribe primitive. */
		UNSUBSCRIBE,
		/** A secure unsubscribe primitive. */
		SECUREUNSUBSCRIBE,
		/** A register primitive. */
		REGISTER,
		/** A request token primitive. */
		REQUESTTOKEN, 
		 /** A secure query primitive. */
		 SECUREQUERY};
	
	/** The header. */
	protected String header = "---SPARQL 1.1 SE Service properties file ---";
	
	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile the properties file
	 * @param secret the secret
	 */
	public SPARQL11SEProperties(String propertiesFile,byte[] secret) {
		super(propertiesFile);
		SEPAEncryption.init(secret);
	}
	
	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile the properties file
	 */
	public SPARQL11SEProperties(String propertiesFile) {
		this(propertiesFile,null);
	}
	
	/* (non-Javadoc)
	 * @see arces.unibo.SEPA.commons.protocol.SPARQL11Properties#defaults()
	 */
	@Override
	protected void defaults() {
		super.defaults();
		properties.setProperty("wsPort", "9000");
		properties.setProperty("subscribePath", "/sparql");
		properties.setProperty("wsScheme", "ws");
		properties.setProperty("registrationPath","/oauth/register");
		properties.setProperty("requestTokenPath","/oauth/token");
	}
	
	/**
	 * Gets the ws port.
	 *
	 * @return the ws port
	 */
	public int getWsPort() {
		return Integer.decode(properties.getProperty("wsPort", "9000"));
	}
	
	/**
	 * Gets the subscribe path.
	 *
	 * @return the subscribe path
	 */
	public String getSubscribePath() {
		return properties.getProperty("subscribePath", "/sparql");
	}
	
	/**
	 * Gets the ws scheme.
	 *
	 * @return the ws scheme
	 */
	public String getWsScheme() {
		return properties.getProperty("wsScheme", "ws");
	}
	
	/**
	 * Gets the registration path.
	 *
	 * @return the registration path
	 */
	public String getRegistrationPath() {
		return properties.getProperty("registrationPath","/oauth/register");
	}

	/**
	 * Gets the request token path.
	 *
	 * @return the request token path
	 */
	public String getRequestTokenPath() {
		return properties.getProperty("requestTokenPath","/oauth/token");
	}
	
	/**
	 * Checks if is token expired.
	 *
	 * @return true, if is token expired
	 */
	public boolean isTokenExpired() {
		long expires = 0;
		if (properties.getProperty("expires") != null) expires = Long.decode(SEPAEncryption.decrypt(properties.getProperty("expires")));
		return (new Date().getTime() >= expires);
	}
	
	/**
	 * Gets the expiring seconds.
	 *
	 * @return the expiring seconds
	 */
	public long getExpiringSeconds() {
		long expires = 0;
		if (properties.getProperty("expires") != null) expires = Long.decode(SEPAEncryption.decrypt(properties.getProperty("expires")));
		long seconds = ((expires - new Date().getTime())/1000);
		if (seconds < 0) seconds = 0;
		return seconds;
	}
	
	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 */
	public String getAccessToken() {
		if (properties.getProperty("jwt") != null) return SEPAEncryption.decrypt(properties.getProperty("jwt"));
		return null;	
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 */
	public String getTokenType() {
		if (properties.getProperty("type") != null) return SEPAEncryption.decrypt(properties.getProperty("type"));
		return null;
	}
	
	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 */
	public String getBasicAuthorization() {
		if (properties.getProperty("client_id") == null || properties.getProperty("client_id") == null ) return null;
		
		try {
			String authorization = new BASE64Encoder().encode((SEPAEncryption.decrypt(properties.getProperty("client_id")) + ":" + SEPAEncryption.decrypt(properties.getProperty("client_secret"))).getBytes("UTF-8"));
			//TODO need a "\n", why?
			return authorization.replace("\n", "");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		}	
		return null;
	}
	
	/**
	 * Sets the credentials.
	 *
	 * @param id the username
	 * @param secret the password
	 */
	public void setCredentials(String id,String secret) {	
		logger.debug("Set credentials Id: "+id+" Secret:"+secret);
		
		//Save on file the encrypted version
		properties.setProperty("client_id", SEPAEncryption.encrypt(id));
		properties.setProperty("client_secret", SEPAEncryption.encrypt(secret));
		storeProperties(propertiesFile);
	}
	
	/**
	 * Sets the JWT.
	 *
	 * @param jwt the JSON Web Token
	 * @param expires the date when the token will expire
	 * @param type the token type (e.g., bearer)
	 */
	public void setJWT(String jwt, Date expires,String type) {		
		//Save on file the encrypted version
		properties.setProperty("jwt", SEPAEncryption.encrypt(jwt));
		properties.setProperty("expires",SEPAEncryption.encrypt(String.format("%d", expires.getTime())));
		properties.setProperty("type", SEPAEncryption.encrypt(type));
		storeProperties(propertiesFile);
	}
	
	/**
	 * The Class SEPAEncryption.
	 */
	private static class SEPAEncryption {
		
		/** The Constant ALGO. */
		//AES 128 bits (16 bytes)
		private static final String ALGO = "AES";
	    
    	/** The key value. */
    	private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm','i', '!', 'I', 'e','2', '3', '7', 'A', 'N' };
	    
    	/** The key. */
    	private static Key key = new SecretKeySpec(keyValue, ALGO);
		
	    /**
    	 * Inits the.
    	 *
    	 * @param secret the secret
    	 */
    	private static void init(byte[] secret) {
	    	if (secret != null && secret.length == 16) keyValue = secret;
	    	key = new SecretKeySpec(keyValue, ALGO);
	    }
	    
	    /**
    	 * Encrypt.
    	 *
    	 * @param Data the data
    	 * @return the string
    	 */
    	public static String encrypt(String Data) {
			try {
				Cipher c = Cipher.getInstance(ALGO);
				c.init(Cipher.ENCRYPT_MODE, key);
				byte[] encVal = c.doFinal(Data.getBytes());
				return new BASE64Encoder().encode(encVal);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
				logger.fatal(e.getMessage());
				return null;
			}
		}
		
		/**
		 * Decrypt.
		 *
		 * @param encryptedData the encrypted data
		 * @return the string
		 */
		public static String decrypt(String encryptedData) {
			try {
				Cipher c = Cipher.getInstance(ALGO);
				c.init(Cipher.DECRYPT_MODE, key);
				byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedData);
				byte[] decValue = c.doFinal(decordedValue);
		        return new String(decValue);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException | IllegalBlockSizeException | BadPaddingException e) {
				logger.fatal(e.getMessage());
				return null;
			}
	    }
	}
}
