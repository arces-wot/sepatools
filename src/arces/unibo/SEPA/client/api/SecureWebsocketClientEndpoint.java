package arces.unibo.SEPA.client.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.tyrus.client.ClientProperties;

import arces.unibo.SEPA.security.SSLSecurityManager;

public class SecureWebsocketClientEndpoint extends WebsocketClientEndpoint {
	protected Logger logger = LogManager.getLogger("SecureWebsocketClientEndpoint");
	private SSLSecurityManager sm = new SSLSecurityManager("sepa.jks","*sepa.jks*","SepaKey","*SepaKey*","SepaCertificate",true,false,null);
	
	public SecureWebsocketClientEndpoint(String wsUrl) {
		super(wsUrl);
			
		client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sm.getWssConfigurator());
	}

}
