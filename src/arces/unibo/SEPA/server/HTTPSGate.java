/* This class implements the W3C SPARQL 1.1 Protocol using SSL 
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

package arces.unibo.SEPA.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class HTTPSGate extends HTTPGate {
	protected Logger logger = LogManager.getLogger("HttpsGate");	
	protected String mBeanObjectName = "arces.unibo.SEPA.server:type=HTTPSGate";
	
	private static int httpsPort = 8443;

	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private String protocol ="SSL";
	
	
	public HTTPSGate(Properties properties, Scheduler scheduler) {
		super(properties, scheduler);
		
		if (properties == null) logger.error("Properties are null");
		else {
			httpsPort = Integer.parseInt(properties.getProperty("httpsPort", "8443"));
		}
	}
	
	@Override
	public void start(){	
		this.setName("SEPA HTTPS Gate");
		
		//Get the MBean server
	    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	    
	    //register the MBean
	    ObjectName name;
		try {
			name = new ObjectName(mBeanObjectName);
			 mbs.registerMBean(this, name);
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e1) {
			logger.error(e1.getMessage());
		}
		
		if (!loadCertificate()) {
			logger.error("Failed to load HTTPS certificate");
			return;
		}
		
		// create HTTPS server
		try {
			server = HttpsServer.create(new InetSocketAddress(httpsPort), 0);
		} catch (IOException e) {
			 logger.error(e.getMessage());
			return;
		}
		
		// create SSL context
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance(protocol);
		} catch (NoSuchAlgorithmException e) {
			 logger.error(e.getMessage());
			return;
		}
		
		// setup the HTTPS context and parameters
		try {
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			 logger.error(e.getMessage());
			return;
		}
		((HttpsServer)server).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
	         public void configure(HttpsParameters params) {
                 try {
                      // Initialize the SSL context
                      SSLContext c = SSLContext.getDefault();
                      SSLEngine engine = c.createSSLEngine();
                      params.setNeedClientAuth(false);
                      params.setCipherSuites(engine.getEnabledCipherSuites());
                      params.setProtocols(engine.getEnabledProtocols());
                      // get the default parameters
                      SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                      params.setSSLParameters(defaultSSLParameters);
                 } catch (Exception ex) {
                	 logger.error(ex.getMessage());
                	 logger.error("Failed to create HTTPS server");
                 }
	         }
		});
		
	    server.createContext("/sparql", new SPARQLHandler());
	    server.createContext("/echo", new EchoHandler()); 
	    server.setExecutor(null);
	    server.start();
	    
	    logger.info("Started on port "+httpsPort);
	}
	
	private boolean loadCertificate() {
		// load certificate
		String keystoreFilename = "sepa.keystore";
		char[] storepass = "SepaKeystorePass2017".toCharArray();
		char[] keypass = "SepaKeystorePass2017".toCharArray();
		String alias = "client";
		FileInputStream fIn;
		try {
			fIn = new FileInputStream(keystoreFilename);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
			return false;
		}
		KeyStore keystore;
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
			keystore.load(fIn, storepass);
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error(e.getMessage());
			return false;
		}
		// display certificate
		Certificate cert;
		try {
			cert = keystore.getCertificate(alias);
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
			kmf.init(keystore, keypass);
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