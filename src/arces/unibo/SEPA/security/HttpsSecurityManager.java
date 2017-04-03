/* This class implements the TLS 1.0 security mechanism used by the HTTPS gate
    
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class HttpsSecurityManager extends HttpsConfigurator {
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	
	private Logger logger = LogManager.getLogger("HttpsSecurityManager");
	
	public HttpsSecurityManager(SSLContext sslContext)  {
		super(sslContext);
		
		// load certificate
		if (!loadCertificate("sepa.jks","SepaKeystore2017","SepaKey2017","SepaKey","SepaCertificate")) {
			logger.error("Failed to load HTTPS certificate");
			return;
		}
		
		// setup the HTTPS context and parameters
		try {
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			 logger.error(e.getMessage());
			return;
		}
	}
	
	@Override
	public void configure(HttpsParameters params) {
        try {
             // Initialize the SSL context
             SSLContext c = getSSLContext();
             SSLEngine engine = c.createSSLEngine();
             params.setNeedClientAuth(false);
             params.setCipherSuites(engine.getEnabledCipherSuites());
             params.setProtocols(engine.getEnabledProtocols());
             
             // get the default parameters
             SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
             params.setSSLParameters(defaultSSLParameters);             
        } catch (Exception ex) {
       	 logger.error(ex.getMessage());
       	 logger.error("Failed to configure SSL connection");
        }
    }
	
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
	
	private boolean storeCertificate(String keystoreFilename,String storePassword,String certificate) {
		try{
		    KeyStore keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());
		     
		    CertAndKeyGen gen = new CertAndKeyGen("RSA","SHA1WithRSA");
		    gen.generate(1024);
		     
		    X509Certificate cert=gen.getSelfCertificate(new X500Name("CN=SINGLE_CERTIFICATE"), (long)365*24*3600);
		     
		    keyStore.setCertificateEntry(certificate, cert);
		     
		    keyStore.store(new FileOutputStream(keystoreFilename), storePassword.toCharArray());
		}catch(Exception ex){
		    logger.error(ex.getMessage());
		    return false;
		}
		
		return true;
	}

	/*
	private Certificate[] loadCertificateChain(String keystoreFilename,String storePassword,String alias) {
		try{
		    KeyStore keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());   
		    return keyStore.getCertificateChain(alias);

		}catch(Exception ex){
		    logger.error(ex.getMessage());
		}
		
		return null;
	}
	
	private Certificate loadCertificate(String keystoreFilename,String storePassword,String alias) { 
		try{
		    KeyStore keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());
		    return keyStore.getCertificate(alias);
		     
		}catch(Exception ex){
		    logger.error(ex.getMessage());
		}
		
		return null;
	}
	*/
	
	private boolean loadCertificate(String keystoreFilename,String storePassword,String keyPassword,String key,String certificate) {
		FileInputStream fIn = null;
		Certificate cert;
		KeyStore keystore;
		
		//Open or create a new JKS keystore+private key+certificate
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
		
		// display certificate
		try {
			cert = keystore.getCertificate(certificate);
		} catch (KeyStoreException e) {
			logger.error(e.getMessage());
			return false;
		}
		logger.debug(cert);
		
		// setup the key manager factory
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
		
		// setup the trust manager factory
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
