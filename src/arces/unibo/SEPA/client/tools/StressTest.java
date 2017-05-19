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
	static Consumer consumer;
	static Producer producer;
	
	static int nConsumers = 1;
	static int nProducers = 1;
	static int producerUpdates = 100;
	
	public static void main(String[] args) {
		properties = new SPARQL11SEProperties("client.json");
		if (!properties.loaded()) {
			logger.fatal("Properties file is null");
			System.exit(-1);
		}
		
		client = new SPARQL11SEProtocol(properties);			
		consumer = new StressTest().new Consumer();
		client.subscribe(new SubscribeRequest("select * where {?s ?p ?o}"),consumer);
		
		producer = new StressTest().new Producer();
		Thread th = new Thread(producer);
		th.setName("Producer");
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
		private Date previous = null;
		private float meanNotificationPeriod = 0;
		private long notifications = 0; 
		private long subscribe = 0; 
		private long unsubscribe = 0; 
		private long ping = 0; 
		private long broken = 0;
		private long error = 0;
		
		@Override
		public void semanticEvent(Notification notify) {
			notifications++;
			if (previous == null) previous = new Date();
			else {
				Date now = new Date();
				logger.info("Notification period "+(now.getTime()-previous.getTime())+" ms");
				meanNotificationPeriod = (meanNotificationPeriod*(notifications-1) + (now.getTime()-previous.getTime()))/notifications;
				previous = now;
			}
			
		}

		@Override
		public void subscribeConfirmed(SubscribeResponse response) {
			subscribe++;
			
		}

		@Override
		public void unsubscribeConfirmed(UnsubscribeResponse response) {
			unsubscribe++;
			
		}

		@Override
		public void ping() {
			ping++;
			
		}

		@Override
		public void brokenSubscription() {
			broken++;
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			error++;
			
		}	
	}
	
	protected class Producer implements Runnable {
		public float meanUpdatePeriod = 0;
		public int nUpdate = 0;
		public long totalTime = 0;
		
		@Override
		public void run() {
			int nUpdate;
			for(nUpdate = 1; nUpdate < producerUpdates; nUpdate++) {
				Date start = new Date();
				client.update(new UpdateRequest("prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \""+String.format("%d", nUpdate)+"\"} where {?s ?p ?o}"));
				Date stop = new Date();
				meanUpdatePeriod = (meanUpdatePeriod*(nUpdate-1) + (stop.getTime()-start.getTime()))/nUpdate;
				
				logger.info("Update "+(stop.getTime()-start.getTime())+" ms Mean: "+meanUpdatePeriod);
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info("Updates [Mean period: "+meanUpdatePeriod+" ms] [Number: "+nUpdate+"]");
			logger.info("Notifications [Mean period: "+consumer.meanNotificationPeriod+" ms] [Number: "+consumer.notifications+"]");
			logger.info("Error [Number: "+consumer.error+"]");
			logger.info("Subscribes [Number: "+consumer.subscribe+"]");
			logger.info("Unsubscribes [Number: "+consumer.unsubscribe+"]");
			logger.info("Ping [Number: "+consumer.ping+"]");
		}
	}
}
