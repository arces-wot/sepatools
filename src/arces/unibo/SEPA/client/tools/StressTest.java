package arces.unibo.SEPA.client.tools;

import java.util.Date;

import arces.unibo.SEPA.client.api.SPARQL11SEProperties;
import arces.unibo.SEPA.client.api.SPARQL11SEProtocol;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.NotificationHandler;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;

public class StressTest extends SEPATest {
	
	public static void main(String[] args) {
		properties = new SPARQL11SEProperties("client.properties");
		if (!properties.loaded()) {
			logger.fatal("Properties file is null");
			System.exit(-1);
		}
		client = new SPARQL11SEProtocol(properties);	
		
		Consumer consumer = new StressTest().new Consumer();
		client.subscribe(new SubscribeRequest("select * where {?s ?p ?o}"),consumer);
		Producer producer = new StressTest().new Producer();
		Thread th = new Thread(producer);
		th.start();
		synchronized(th){
			try {
				th.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	protected class Consumer implements NotificationHandler {
		Date previous = null;
		
		@Override
		public void semanticEvent(Notification notify) {
			if (previous == null) previous = new Date();
			else {
				Date now = new Date();
				logger.info("Notification period "+(now.getTime()-previous.getTime())+" ms");
				previous = now;
			}
			
		}

		@Override
		public void subscribeConfirmed(SubscribeResponse response) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unsubscribeConfirmed(UnsubscribeResponse response) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void ping() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void brokenSubscription() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			// TODO Auto-generated method stub
			
		}
		
	}
	protected class Producer implements Runnable {
		@Override
		public void run() {
			int i=0;
			while(i<1000) {
				Date start = new Date();
				client.update(new UpdateRequest("prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \""+String.format("%d", i++)+"\"} where {?s ?p ?o}"));
				Date stop = new Date();
				logger.info("Update "+(stop.getTime()-start.getTime())+" ms");
			}	
		}
	}
}
