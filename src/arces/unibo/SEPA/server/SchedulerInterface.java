package arces.unibo.SEPA.server;

import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.server.RequestResponseHandler.ResponseAndNotificationListener;

public interface SchedulerInterface {

	Integer getToken();
	void addRequest(Request request, ResponseAndNotificationListener listener);
	void releaseToken(Integer token);
}
