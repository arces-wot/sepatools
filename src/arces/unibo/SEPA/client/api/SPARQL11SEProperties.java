package arces.unibo.SEPA.client.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Date;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sun.misc.*;

public class SPARQL11SEProperties extends SPARQL11Properties {
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProperties");
	private static final long serialVersionUID = 6235191535738638847L;
	
	/**
	 * The new primitives introduced by the SPARQL 1.1 SE Protocol are: 
	 * 
	 * SECUREUPDATE,SECUREQUERY,SUBSCRIBE,SECURESUBSCRIBE,UNSUBSCRIBE,SECUREUNSUBSCRIBE,REGISTER,REQUESTTOKEN
	 * 
	 * 
	* @author Luca Roffia (luca.roffia@unibo.it)
	* @version 0.1
	* */
	public enum SPARQL11SEPrimitive {SECUREUPDATE,SUBSCRIBE,SECURESUBSCRIBE,UNSUBSCRIBE,SECUREUNSUBSCRIBE,REGISTER,REQUESTTOKEN, SECUREQUERY};
	
	protected String header = "---SPARQL 1.1 SE Service properties file ---";
	
	//Properties class fields
	private int wsPort = 9000;
	private String subscribePath = "/sparql";
	private String wsScheme = "ws";
	private String registrationPath = "/oauth/register";
	private String requestTokenPath = "/oauth/token";
	
	//Security credentials
	private String client_id = null;
	private String client_secret = null;
	private String jwt = null;
	private long expires = 0;
	private String tokenType = null;
	
	public SPARQL11SEProperties(String propertiesFile,byte[] secret) {
		super(propertiesFile);
		SEPAEncryption.init(secret);
		
		wsPort = Integer.decode(getProperty("wsPort", "9000"));
		subscribePath = getProperty("subscribePath", "/sparql");
		wsScheme = getProperty("wsScheme", "ws");
		registrationPath = getProperty("registrationPath","/oauth/register");
		requestTokenPath = getProperty("requestTokenPath","/oauth/token");
		
		// Decrypt credentials
		if (getProperty("client_id") != null) client_id = SEPAEncryption.decrypt(getProperty("client_id"));
		if (getProperty("client_secret") != null) client_secret = SEPAEncryption.decrypt(getProperty("client_secret"));
		if (getProperty("jwt") != null) jwt = SEPAEncryption.decrypt(getProperty("jwt"));
		if (getProperty("expires") != null) expires = Long.decode(SEPAEncryption.decrypt(getProperty("expires")));
		if (getProperty("type") != null) tokenType = SEPAEncryption.decrypt(getProperty("type"));
	}
	
	public SPARQL11SEProperties(String propertiesFile) {
		this(propertiesFile,null);
	}
	
	@Override
	protected void defaults() {
		super.defaults();
		setProperty("wsPort", "9000");
		setProperty("subscribePath", "/sparql");
		setProperty("wsScheme", "ws");
		setProperty("registrationPath","/oauth/register");
		setProperty("requestTokenPath","/oauth/token");
	}
	
	public int getWsPort() {
		return wsPort;
	}
	
	public String getSubscribePath() {
		return subscribePath;
	}
	
	public String getWsScheme() {
		return wsScheme;
	}
	
	public String getRegistrationPath() {
		return registrationPath;
	}

	public String getRequestTokenPath() {
		return requestTokenPath;
	}
	
	public boolean isTokenExpired() {		
		return (new Date().getTime() >= expires);
	}
	
	public long getExpiringSeconds() {
		long seconds = ((expires - new Date().getTime())/1000);
		if (seconds < 0) seconds = 0;
		return seconds;
	}
	
	public String getAccessToken() {
		return jwt;	
	}

	public String getTokenType() {
		return tokenType;
	}
	
	public String getBasicAuthorization() {
		if (client_id != null && client_secret != null)
			try {
				String authorization = new BASE64Encoder().encode((client_id + ":" + client_secret).getBytes("UTF-8"));
				return authorization.replace("\n", "");
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
			}	
		return null;
	}
	
	public void setCredentials(String id,String secret) {	
		logger.debug("Set credentials Id: "+id+" Secret:"+secret);
		client_id = id;
		client_secret = secret;
		
		//Save on file the encrypted version
		setProperty("client_id", SEPAEncryption.encrypt(client_id));
		setProperty("client_secret", SEPAEncryption.encrypt(client_secret));
		storeProperties();
	}
	
	public void setJWT(String jwt, Date expires,String type) {
		this.jwt = jwt;
		this.expires = expires.getTime();
		this.tokenType =type;
		
		//Save on file the encrypted version
		setProperty("jwt", SEPAEncryption.encrypt(jwt));
		setProperty("expires",SEPAEncryption.encrypt(String.format("%d", expires.getTime())));
		setProperty("type", SEPAEncryption.encrypt(type));
		storeProperties();
	}
	
	private static class SEPAEncryption {
		//AES 128 bits (16 bytes)
		private static final String ALGO = "AES";
	    private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm','i', '!', 'I', 'e','2', '3', '7', 'A', 'N' };
	    private static Key key = new SecretKeySpec(keyValue, ALGO);
		
	    private static void init(byte[] secret) {
	    	if (secret != null && secret.length == 16) keyValue = secret;
	    	key = new SecretKeySpec(keyValue, ALGO);
	    }
	    
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
