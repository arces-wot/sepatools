package arces.unibo.SEPA.commons.response;

public interface NotificationHandler {
	public void semanticEvent(Notification notify);
	public void subscribeConfirmed(SubscribeResponse response);
	public void unsubscribeConfirmed(UnsubscribeResponse response);
	public void ping();
	public void brokenSubscription();
	public void onError(ErrorResponse errorResponse);
}
