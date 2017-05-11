package arces.unibo.SEPA.scheduling;

import arces.unibo.SEPA.commons.request.Request;
import arces.unibo.SEPA.scheduling.RequestResponseHandler.ResponseAndNotificationListener;

public interface SchedulerInterface {

	int getToken();
	void addRequest(Request request, ResponseAndNotificationListener listener);
	void releaseToken(Integer token);
}
