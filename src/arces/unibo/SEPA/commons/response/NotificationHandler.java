package arces.unibo.SEPA.commons.response;

public interface NotificationHandler {
	public void semanticEvent(Notification notify);
	public void subscribeConfirmed(String spuid);
	public void unsubscribeConfirmed(String spuid);
	public void ping();
	public void brokenSubscription();
}
