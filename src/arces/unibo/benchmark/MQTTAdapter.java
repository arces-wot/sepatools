package arces.unibo.benchmark;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Producer;
import arces.unibo.SEPA.commons.Bindings;
import arces.unibo.SEPA.commons.RDFTermLiteral;
import arces.unibo.SEPA.commons.RDFTermURI;
import arces.unibo.SEPA.application.ApplicationProfile;
import arces.unibo.SEPA.application.Logger.VERBOSITY;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

public class MQTTAdapter extends Producer implements MqttCallback {
	private MqttClient mqttClient;
	private String serverURI = "ssl://giove.mars:8883";
	private static String clientID = "MQTTAdapter";
	private String[] topicsFilter = {"arces/servers/#"};
	static MQTTAdapter adapter;
	private boolean created = false;
	
	public static HashMap<String,String> debugHash = new HashMap<String,String>();
 	
	public static class SslUtil {

	    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
	                                                    final String password) {
	        try {

	            /**
	             * Add BouncyCastle as a Security Provider
	             */
	            Security.addProvider(new BouncyCastleProvider());

	            JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");

	            /**
	             * Load Certificate Authority (CA) certificate
	             */
	            PEMParser reader = new PEMParser(new FileReader(caCrtFile));
	            X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
	            reader.close();

	            X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);

	            /**
	             * Load client certificate
	             */
	            reader = new PEMParser(new FileReader(crtFile));
	            X509CertificateHolder certHolder = (X509CertificateHolder) reader.readObject();
	            reader.close();

	            X509Certificate cert = certificateConverter.getCertificate(certHolder);

	            /**
	             * Load client private key
	             */
	            reader = new PEMParser(new FileReader(keyFile));
	            Object keyObject = reader.readObject();
	            reader.close();

	            PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
	            JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

	            KeyPair key;

	            if (keyObject instanceof PEMEncryptedKeyPair) {
	                key = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
	            } else {
	                key = keyConverter.getKeyPair((PEMKeyPair) keyObject);
	            }

	            /**
	             * CA certificate is used to authenticate server
	             */
	            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	            caKeyStore.load(null, null);
	            caKeyStore.setCertificateEntry("ca-certificate", caCert);

	            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
	                    TrustManagerFactory.getDefaultAlgorithm());
	            trustManagerFactory.init(caKeyStore);

	            /**
	             * Client key and certificates are sent to server so it can authenticate the client
	             */
	            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	            clientKeyStore.load(null, null);
	            clientKeyStore.setCertificateEntry("certificate", cert);
	            clientKeyStore.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
	                    new Certificate[]{cert});

	            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
	                    KeyManagerFactory.getDefaultAlgorithm());
	            keyManagerFactory.init(clientKeyStore, password.toCharArray());

	            /**
	             * Create SSL socket factory
	             */
	            SSLContext context = SSLContext.getInstance("TLSv1.2");
	            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

	            /**
	             * Return the newly created socket factory object
	             */
	            return context.getSocketFactory();

	        } catch (Exception e) {
	            Logger.log(VERBOSITY.ERROR, clientID, e.getMessage());
	        }

	        return null;
	    }
	}
	
	public MQTTAdapter(ApplicationProfile appProfile, String updateID) {
		super(appProfile, updateID);
		
		try 
		{
			mqttClient = new MqttClient(serverURI,clientID);
		} 
		catch (MqttException e) {
			Logger.log(VERBOSITY.FATAL,clientID,"Failed to create MQTT client "+e.getMessage());
			return ;
		}
		
		try 
		{
			MqttConnectOptions options = new MqttConnectOptions();
			SSLSocketFactory ssl = SslUtil.getSocketFactory("/usr/local/mosquitto-certs/ca.crt", "/usr/local/mosquitto-certs/mml.crt", "/usr/local/mosquitto-certs/mml.key", "");
			if (ssl == null) {
				Logger.log(VERBOSITY.ERROR, clientID, "SSL security option creation failed");
			}
			else options.setSocketFactory(ssl);
			mqttClient.connect(options);
		} 
		catch (MqttException e) {
			Logger.log(VERBOSITY.FATAL,clientID,"Failed to connect "+e.getMessage());
			return ;
		}
		
		mqttClient.setCallback(this);
		
		try 
		{
			mqttClient.subscribe(topicsFilter);
		} 
		catch (MqttException e) {
			Logger.log(VERBOSITY.FATAL,clientID,"Failed to subscribe "+e.getMessage());
			return ;
		}
		
		String topics = "";
		for (int i=0; i < topicsFilter.length;i++) topics += "\""+ topicsFilter[i] + "\" ";
		
		Logger.log(VERBOSITY.INFO,clientID,"MQTT client "+clientID+" subscribed to "+serverURI+" Topic filter "+topics);
	
		created = true;
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage value) throws Exception {
		Logger.log(VERBOSITY.DEBUG,clientID,topic+ " "+value.toString());
		
		String node = "iot:"+topic.replace('/', '_');
		String temperature = value.toString();
		Bindings bindings = new Bindings();
		bindings.addBinding("node", new RDFTermURI(node));
		bindings.addBinding("value", new RDFTermLiteral(temperature));
		adapter.update(bindings);
		
		if (debugHash.containsKey(node)) {
			if (!debugHash.get(node).equals(temperature)) {
				Logger.log(VERBOSITY.DEBUG,clientID,topic+ " "+debugHash.get(node)+"-->"+temperature.toString());	
			}
		}
		
		debugHash.put(node, temperature);
	}
	
	public boolean join() {
		if (!super.join()) return false;
		else return created;
	}

	public static void main(String[] args) {
		Logger.loadSettings();
		
		ApplicationProfile profile = new ApplicationProfile();
		if(!profile.load("MQTTAdapter.sap")) return;
		
		adapter = new MQTTAdapter(profile,"UPDATE");
		
		if (!adapter.join()) return;
		
		Logger.log(VERBOSITY.INFO,clientID,"Press any key to exit...");
		
		try {
			System.in.read();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
