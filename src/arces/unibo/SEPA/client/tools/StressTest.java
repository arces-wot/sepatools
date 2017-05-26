package arces.unibo.SEPA.client.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.client.pattern.GenericClient;
import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;
import arces.unibo.SEPA.commons.SPARQL.RDFTermLiteral;
import arces.unibo.SEPA.commons.SPARQL.RDFTermURI;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;

public class StressTest {
	private static final Logger logger = LogManager.getLogger("StressTest");
	static int nConsumers = 2;
	static int nProducers = 50;
	static int producerUpdates = 50;

	static CountDownLatch publishingEnded = new CountDownLatch(nProducers * producerUpdates);
	static CountDownLatch notificationEnded = new CountDownLatch(nProducers * producerUpdates * nConsumers);

	private static HashMap<Integer, Float> meanNotificationPeriod = new HashMap<Integer, Float>();
	private static HashMap<Integer, Integer> notifications = new HashMap<Integer, Integer>();

	private static HashMap<Integer, Float> meanUpdatePeriod = new HashMap<Integer, Float>();
	private static HashMap<Integer, Integer> updates = new HashMap<Integer, Integer>();

	private static synchronized void notification(int index, float period) {
		Integer number;
		if ((number = notifications.get(index)) != null) {
			notifications.put(index, number + 1);
			float mean = (meanNotificationPeriod.get(index) * (notifications.get(index) - 1) + period)
					/ notifications.get(index);
			meanNotificationPeriod.put(index, mean);
		} else {
			notifications.put(index, 1);
			meanNotificationPeriod.put(index, period);
		}
		notificationEnded.countDown();
		logger.info("*** NOTIFICATION *** #"+index+" "+period+" ms ("+notificationEnded.getCount()+")");
	}

	private static synchronized void update(int index, float period) {
		Integer number;
		if ((number = updates.get(index)) != null) {
			updates.put(index, number + 1);
			float mean = (meanUpdatePeriod.get(index) * (updates.get(index) - 1) + period) / updates.get(index);
			meanUpdatePeriod.put(index, mean);
		} else {
			updates.put(index, 1);
			meanUpdatePeriod.put(index, period);
		}
		publishingEnded.countDown();
		logger.info("*** UPDATE *** #"+index+" "+period+" ms ("+publishingEnded.getCount()+")");
	}

	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException, IllegalArgumentException, URISyntaxException {
		for (int i = 0; i < nConsumers; i++) {
			new Thread(new StressTest().new Subscriber("client.jpar", i)).start();
		}

		for (int i = 0; i < nProducers; i++) {
			new Thread(new StressTest().new Publisher("client.jpar", i, producerUpdates)).start();
		}

		try {
			notificationEnded.await();
		} catch (InterruptedException e) {
		}

		logger.info("UPDATES:" + updates.toString());
		logger.info("NOTIFICATIONS:" + notifications.toString());
		logger.info("UPDATE PERIOD:" + meanUpdatePeriod.toString());
		logger.info("NOTIFICATION PERIOD:" + meanNotificationPeriod.toString());
	}

	class Subscriber extends GenericClient implements Runnable {
		private Date previous = null;
		private int index = 0;

		public Subscriber(String jparFile, int i)
				throws IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException, URISyntaxException {
			super(jparFile);
			index = i;
			previous = new Date();
			update("delete {?s ?p ?o} where {?s ?p ?o}",null);
			subscribe("select * where {?s ?p ?o}", null);
		}

		@Override
		public void semanticEvent(Notification notify) {
			Date now = new Date();
			float period = now.getTime() - previous.getTime();
			StressTest.notification(index, period);
			previous = now;
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {}

		@Override
		public void brokenSubscription() {}

		@Override
		public void onError(ErrorResponse errorResponse) {}

		@Override
		public void run() {}
	}

	class Publisher extends GenericClient implements Runnable {
		private int index = 0;
		public int nUpdate = 0;

		public Publisher(String jparFile, int i, int nu)
				throws IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException {
			super(jparFile);
			index = i;
			nUpdate = nu;
		}

		@Override
		public void run() {
			String id = UUID.randomUUID().toString();
			String UPDATE = "prefix test:<http://www.vaimee.com/test#> delete {?id test:value ?oldValue} insert {?ide test:value ?value} where {OPTIONAL{?id test:value ?oldValue}}";
			Integer i;
			Bindings bindings = new Bindings();
			bindings.addBinding("id", new RDFTermURI("test:" + id));
			for (i = 0; i < nUpdate; i++) {
				bindings.addBinding("value", new RDFTermLiteral(i.toString()));
				Date start = new Date();
				update(UPDATE, bindings);
				Date stop = new Date();
				StressTest.update(index, stop.getTime() - start.getTime());
				/*try {
					Thread.sleep((long) (Math.random() * 1000));
				} catch (InterruptedException e) {
				}*/
			}
		}

		@Override
		public void notify(ARBindingsResults notify, String spuid, Integer sequence) {}

		@Override
		public void notifyAdded(BindingsResults bindingsResults, String spuid, Integer sequence) {}

		@Override
		public void notifyRemoved(BindingsResults bindingsResults, String spuid, Integer sequence) {}

		@Override
		public void onSubscribe(BindingsResults bindingsResults, String spuid) {}

		@Override
		public void brokenSubscription() {}

		@Override
		public void onError(ErrorResponse errorResponse) {}
	}
}
