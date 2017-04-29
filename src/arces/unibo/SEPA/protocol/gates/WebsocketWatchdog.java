package arces.unibo.SEPA.protocol;

import arces.unibo.SEPA.protocol.SecureEventProtocol.SUBSCRIPTION_STATE;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

class SocketWatchdog extends Thread {
	
	private long pingPeriod = 0;
	private long firstPing = 0;
	private long DEFAULT_PING_PERIOD = 5000;
	private long DEFAULT_SUBSCRIPTION_DELAY = 5000;
	
	private boolean pingReceived = false;		
	private SUBSCRIPTION_STATE state = SUBSCRIPTION_STATE.UNSUBSCRIBED;
	
	private static final Logger logger = LogManager.getLogger("Watchdog");
	
	private NotificationHandler handler = null;
	private SEPAEndpoint wsClient;
	private String sparql;
	
	public SocketWatchdog(NotificationHandler handler, SEPAEndpoint wsClient,String sparql) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
	}
	
	public synchronized void ping() {
		logger.debug("Ping!");
		pingReceived = true;
		if (firstPing == 0) firstPing = System.currentTimeMillis();
		else {
			pingPeriod = System.currentTimeMillis() - firstPing;	
			firstPing = 0;
			logger.debug("Ping period: "+pingPeriod);
		}
		notifyAll();
	}
	
	public void subscribed() {
		state = SUBSCRIPTION_STATE.SUBSCRIBED;
		logger.debug("Subscribed");
	}
	
	public void unsubscribed() {
		state = SUBSCRIPTION_STATE.UNSUBSCRIBED;
		logger.debug("Unsubscribed");
	}
	
	private synchronized boolean waitPing() {
		logger.debug("Wait ping...");
		pingReceived = false;
		try {
			if (pingPeriod != 0) wait(pingPeriod*3/2);
			else wait(DEFAULT_PING_PERIOD*3/2);
		} catch (InterruptedException e) {

		}	
		return pingReceived;
	}
	
	private synchronized boolean subscribing() {
		logger.debug("Subscribing...");
		if (wsClient == null) {
			logger.warn("Websocket client is null");
			return false;
		}
		while(state == SUBSCRIPTION_STATE.BROKEN_SOCKET) {
			if (wsClient.isConnected()) wsClient.close();
			wsClient.subscribe(sparql,handler);
			try {
				wait(DEFAULT_SUBSCRIPTION_DELAY);
			} catch (InterruptedException e) {

			}
		}
		return (state == SUBSCRIPTION_STATE.SUBSCRIBED);
	}
	
	public void run() {
		try {
			Thread.sleep(DEFAULT_PING_PERIOD*5/2);
		} catch (InterruptedException e) {
			return;
		}
		
		while(true){
			while (waitPing()) {}
			
			if (state == SUBSCRIPTION_STATE.SUBSCRIBED) {
				if (handler != null) handler.brokenSubscription();
				state = SUBSCRIPTION_STATE.BROKEN_SOCKET;
			}
			
			if(!subscribing()) return;
		}
	}
}
