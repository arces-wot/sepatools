package arces.unibo.SEPA.client.api;

import arces.unibo.SEPA.commons.SPARQL.EndpointProperties;

public class ClientProperties extends EndpointProperties {

	private static final long serialVersionUID = 6235191535738638847L;
	
	public ClientProperties(String propertiesFile) {
		super(propertiesFile);
	}
	
	@Override
	protected void defaults() {
		super.defaults();
		setProperty("wsPort", "9000");
		setProperty("subscribePath", "/sparql");
		setProperty("wsScheme", "ws");
	}
	
	public int getWsPort() {
		return Integer.decode(getProperty("wsPort", "9000"));
	}
	
	public String getSubscribePath() {
		return getProperty("subscribePath", "/sparql");
	}
	
	public String getWsScheme() {
		return getProperty("wsScheme", "ws");
	}
}
