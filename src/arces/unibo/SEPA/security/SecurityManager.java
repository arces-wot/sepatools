/* This class implements the TLS 1.0 security mechanism used by the HTTPS and WSS gates
    
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

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import com.sun.net.httpserver.HttpsConfigurator;

public class SecurityManager {
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private SSLContext sslContext;
	private String protocol =  "TLSv1";
		
	//HTTPS
	private SEPAHttpsConfigurator httpsConfig;
	
	//WSS
	private SEPAWssConfigurator wssConfig;
	
	private Logger logger = LogManager.getLogger("SecurityManager");
	
	public class SEPAWssConfigurator extends SSLEngineConfigurator {
		public SEPAWssConfigurator(SSLContext sslContext) {
			super(sslContext,false,false,false);
		}
	}
	
	public class SEPAHttpsConfigurator extends HttpsConfigurator {

		public SEPAHttpsConfigurator(SSLContext sslContext) {
			super(sslContext);
		}
	}
	
	public SecurityManager()  {	
		// Load certificate
		if (!loadCertificate("sepa.jks","SepaKeystore2017","SepaKey2017","SepaKey","SepaCertificate")) {
			logger.error("Failed to load HTTPS certificate");
			return;
		}
		
		// Create SSL context 
		try {
			sslContext = SSLContext.getInstance(protocol);
		} catch (NoSuchAlgorithmException e) {
			 logger.error(e.getMessage());
			return;
		}	
		try {
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			 logger.error(e.getMessage());
			return;
		}
		
		httpsConfig = new SEPAHttpsConfigurator(sslContext);
		
		wssConfig =  new SEPAWssConfigurator(sslContext);
	}
	
	public SEPAHttpsConfigurator getHttpsConfigurator(){
		return httpsConfig;
	}
	
	public SEPAWssConfigurator getWssConfigurator() {
		return wssConfig;
	}
	
	/*
	private boolean createStore(String keystoreFilename,String storePassword) {
		//Create keystore
		try{
		    KeyStore keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(null,null);
		     
		    keyStore.store(new FileOutputStream(keystoreFilename), storePassword.toCharArray());
		}catch(Exception ex){
		    logger.error(ex.getMessage());
		    return false;
		}
		
		return true;
	}
	
	
	private boolean storePrivateKey(String keystoreFilename,String storePassword,String keyPassword,String alias){
		//Store private key
		try{
		    KeyStore keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());
		     
		    CertAndKeyGen gen = new CertAndKeyGen("RSA","SHA1WithRSA");
		    gen.generate(1024);
		     
		    Key key=gen.getPrivateKey();
		    X509Certificate cert=gen.getSelfCertificate(new X500Name("CN=ROOT"), (long)365*24*3600);
		     
		    X509Certificate[] chain = new X509Certificate[1];
		    chain[0]=cert;
		     
		    keyStore.setKeyEntry(alias, key, keyPassword.toCharArray(), chain);
		     
		    keyStore.store(new FileOutputStream(keystoreFilename), storePassword.toCharArray());
		}catch(Exception ex){
			logger.error(ex.getMessage());
			return false;
		}
		
		return true;
	}
	
	private boolean storeCertificate(String keystoreFilename,String storePassword,String alias) {
		try{
		    KeyStore keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());
		     
		    
		    CertAndKeyGen gen = new CertAndKeyGen("RSA","SHA1WithRSA");
		    gen.generate(1024);
		     
		    X509Certificate cert=gen.getSelfCertificate(new X500Name("CN=SINGLE_CERTIFICATE"), (long)365*24*3600);
		    
		    //Certificate cert = keyStore.getCertificate(alias);
	    
		    keyStore.setCertificateEntry(alias, cert);
		     
		    keyStore.store(new FileOutputStream(keystoreFilename), storePassword.toCharArray());
		}catch(Exception ex){
		    logger.error(ex.getMessage());
		    return false;
		}
		
		return true;
	}
	*/
	private boolean loadCertificate(String keystoreFilename,String storePassword,String keyPassword,String key,String certificate) {
		
		KeyStore keystore = null;
		
		try {
			keystore = KeyStore.getInstance("JKS");
		} catch (KeyStoreException e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		}
		
		try {
			keystore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		}
		 
		/*
		//Open or create a new JKS key store
		try {
			fIn = new FileInputStream(keystoreFilename);
		} catch (FileNotFoundException e) {
			if (!createStore(keystoreFilename,storePassword)) {
				logger.error("Failed to create JKS keystore: "+keystoreFilename);
				return false;
			}
			if (!storePrivateKey(keystoreFilename,storePassword,keyPassword,key)) {
				logger.error("Failed to store private key: "+key);
				return false;
			}
			if (!storeCertificate(keystoreFilename,storePassword,certificate)) {
				logger.error("Failed to store certificate: "+certificate);
				return false;
			}
		}	
		
		try {
			keystore = KeyStore.getInstance("JKS");
		} catch (KeyStoreException e) {
				logger.error(e.getMessage());
			try {
				fIn.close();
			} catch (IOException e1) {
				logger.error(e1.getMessage());
			}
			return false;
		}
		try {
			keystore.load(fIn, storePassword.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error(e.getMessage());
			return false;
		}
		*/
		
		// Setup the key manager factory
		try {
			kmf = KeyManagerFactory.getInstance("SunX509");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}
		try {
			kmf.init(keystore, keyPassword.toCharArray());
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		// Setup the trust manager factory
		try {
			tmf = TrustManagerFactory.getInstance("SunX509");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}
		try {
			tmf.init(keystore);
		} catch (KeyStoreException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		return true;
	}
}
