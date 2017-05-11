package arces.unibo.SEPA.client.tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import arces.unibo.SEPA.client.api.SPARQL11SEProperties;
import arces.unibo.SEPA.client.api.SPARQL11SEProtocol;

import arces.unibo.SEPA.commons.SPARQL.Bindings;

import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.request.UpdateRequest;
import arces.unibo.SEPA.commons.response.ErrorResponse;
import arces.unibo.SEPA.commons.response.Notification;
import arces.unibo.SEPA.commons.response.NotificationHandler;
import arces.unibo.SEPA.commons.response.QueryResponse;
import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.SubscribeResponse;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;

public class SEPATest {
	private static final Logger logger = LogManager.getLogger("SEPATest");
	private static Results results = new SEPATest().new Results();
	private static TestNotificationHandler handler = new TestNotificationHandler();
	
	//Subscription variables
	private static boolean subscriptionConfirmed = false;
	private static boolean unsubscriptionConfirmed = false;
	private static String spuid = null;
	private static boolean pingReceived = false;
	private static boolean notificationReceived = false;
	private static String errorReceived = null;
	private static Object sync = new Object();
	private static boolean errorNotification = false;
	
	private static SPARQL11SEProtocol client;
	private static SPARQL11SEProperties properties;
	
	private static final long subscribeConfirmDelay = 2000;
	private static final long pingDelay = 5000;
	
	private class Results {
		private long failed;
		private ArrayList<Result> results = new  ArrayList<Result>();
		
		public void addResult(String title,boolean success) {
			results.add(new Result(title,success));
			if (!success) failed++;
		}
		
		public void print() {
			if (failed>0) logger.error("*** TEST FAILED ("+failed+"/"+results.size()+") ***");
			else logger.warn("*** ვაიმეე TEST PASSED ("+results.size()+") ვაიმეე ***");
			int index = 1;
			for (Result res : results) {
				res.print(index++);
			}
		}
	}
	
	private class Result {
		private String title;
		private boolean success;
		
		public Result(String title,boolean success) {
			this.title = title;
			this.success = success;
		}
		
		public String toString() {
			if (success) title = title + " [PASSED]";
			else		 title = title + " [FAILED]";
			return title;
		}
		
		public void print(int index) {
			if (success) logger.warn(index+ " " +toString());
			else logger.error(index + " " +toString());
		}
	}
	public static class TestNotificationHandler implements NotificationHandler {

		@Override
		public void semanticEvent(Notification notify) {
			synchronized(sync) {
				logger.debug(notify.toString());
				notificationReceived = true;
				if (subscriptionConfirmed) sync.notify();
			}
		}

		@Override
		public void ping() {
			synchronized(sync) {
				logger.debug(new Date() + " Ping");
				pingReceived = true;
				sync.notify();
			}
		}

		@Override
		public void brokenSubscription() {
			logger.debug("Broken subscription");
		}

		@Override
		public void subscribeConfirmed(SubscribeResponse response) {
			synchronized(sync) {
				logger.debug(response.toString());
				spuid = response.getSpuid();
				subscriptionConfirmed = true;
				sync.notify();
			}
		}

		@Override
		public void unsubscribeConfirmed(UnsubscribeResponse response) {
			synchronized(sync) {
				logger.debug(response.toString());
				unsubscriptionConfirmed = true;
				sync.notify();
			}
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			synchronized(sync) {
				logger.debug(errorResponse.toString());	
				errorReceived = errorResponse.toString();
				errorNotification = true;
				sync.notify();
			}
		}
	}
	
	private static boolean updateTest(String sparql,boolean secure,boolean tokenExpired) {
		UpdateRequest update = new UpdateRequest(sparql);
		
		if (!secure) logger.info(update.toString());
		else logger.info("SECURE "+update.toString());
		
		Response response;
		if (secure) response = client.secureUpdate(update);
		else response = client.update(update);
		
		boolean error = response.getClass().equals(ErrorResponse.class);
		
		if ((!secure && !error) || (secure && error && tokenExpired) || (secure && !error && !tokenExpired)){
			logger.warn("PASSED "+response.toString());
			return true;
		} 
				
		logger.error("FAILED "+response.toString());
		return false;
	}
	
	private static boolean queryTest(String sparql,String utf8,boolean secure,boolean tokenExpired) {
		QueryRequest query = new QueryRequest(sparql);
		
		if (!secure) logger.info(query.toString());
		else logger.info("SECURE "+query.toString());
				
		Response response;
		if (!secure) response = client.query(query);
		else response = client.secureQuery(query);
		
		boolean error = response.getClass().equals(ErrorResponse.class);
		boolean utf8Test = false;
		
		if (!error && utf8 != null) {
			QueryResponse queryResponse = (QueryResponse) response;
			List<Bindings> results = queryResponse.getBindingsResults().getBindings();
			if (results.size() == 1) {
				Bindings bindings = results.get(0);
				if(bindings.isLiteral("o")) {
					String value = bindings.getBindingValue("o");
					if (value.equals(utf8)) {
						logger.info("UTF-8 test PASSED ვაიმეე!");
						utf8Test = true;
					}
				}
			}	
		}
		
		if ((!secure && utf8Test) || (secure && error && tokenExpired) || (secure && !error && !tokenExpired)) {
			logger.warn("PASSED "+response.toString());
			return true;
		}
	
		logger.error("FAILED "+response.toString());
		return false;
	}
	
	private static boolean subscribeTest(String sparql,boolean secure,boolean tokenExpired) {
		SubscribeRequest sub = new SubscribeRequest(sparql);

		if (secure) logger.info("SECURE "+sub.toString());
		else logger.info(sub.toString());

		Response response;
		subscriptionConfirmed = false;
		notificationReceived = false;
		if (!secure) response = client.subscribe(sub, handler);
		else response = client.secureSubscribe(sub, handler);
		
		boolean error = response.getClass().equals(ErrorResponse.class);
		
		if (secure && error && tokenExpired) {
			logger.warn("PASSED "+ response.toString());
			return true;
		}
		
		if (!secure && !error ) {
			logger.warn("PASSED "+ response.toString());
			return true;
		}
		
		if (secure && !error && !tokenExpired) {
			logger.warn("PASSED "+ response.toString());
			return true;
		}
		

		logger.error("FAILED "+response.toString());
		return false;
	}
	
	private static boolean waitSubscribeConfirm(){	
		synchronized(sync) {
			try {
				sync.wait(subscribeConfirmDelay);
			} catch (InterruptedException e) {
				logger.info("InterruptedException: "+e.getMessage());
			}
			catch (IllegalStateException e) {
				logger.error("IllegalStateException: "+e.getMessage());
			}
			catch (IllegalMonitorStateException e) {
				logger.error("IllegalMonitorStateException: "+e.getMessage());
			}	
		}
		
		if (subscriptionConfirmed) {
			logger.warn("PASSED");
			return true;
		}		

		if (errorNotification) logger.error(errorReceived);
		else logger.error("FAILED");
			
		return false;
	}
	
	private static boolean pingTest() {
		long delay = pingDelay+(pingDelay/2);
		synchronized(sync) {
			try {
				sync.wait(delay);
			} catch (InterruptedException e) {
				logger.info("InterruptedException: "+e.getMessage());
			}
			catch (IllegalStateException e) {
				logger.error("IllegalStateException: "+e.getMessage());
			}
			catch (IllegalMonitorStateException e) {
				logger.error("IllegalMonitorStateException: "+e.getMessage());
			}	
		}	
		if (!pingReceived) {
			logger.error("FAILED");
			return false;
		}
		else {
			logger.warn("PASSED");
			return true;
		}
	}
			
	private static boolean waitNotification() {
		synchronized(sync) {
			try {
				sync.wait(subscribeConfirmDelay);
			} catch (InterruptedException e) {
				logger.info("InterruptedException: "+e.getMessage());
			}
			catch (IllegalStateException e) {
				logger.error("IllegalStateException: "+e.getMessage());
			}
			catch (IllegalMonitorStateException e) {
				logger.error("IllegalMonitorStateException: "+e.getMessage());
			}	
		}	
		if (!notificationReceived) {
			logger.error("FAILED");
			return false;
		}
		else {
			logger.warn("PASSED");
			return true;
		}
	}
	
	private static boolean unsubscribeTest(String spuid,boolean secure,boolean tokenExpired) {
		unsubscriptionConfirmed = false;
		
		UnsubscribeRequest unsub = new UnsubscribeRequest(spuid);

		Response response;
		if (!secure) response = client.unsubscribe(unsub);
		else response = client.secureUnsubscribe(unsub);
		
		boolean error = response.getClass().equals(ErrorResponse.class);
		if (!secure && error) {
			logger.error(response.toString());
			return false;
		}
		
		if (secure && error && !tokenExpired) {
			logger.error(response.toString());
			return false;
		}
		
		logger.warn("PASSED");
		return true;
	}
	
	private static boolean waitUnsubscribeConfirm() {
		synchronized(sync) {
			try {
				sync.wait(5000);
			} catch (InterruptedException e) {
				logger.debug("InterruptedException: "+e.getMessage());
			}
			catch (IllegalStateException e) {
				logger.debug("IllegalStateException: "+e.getMessage());
			}
			catch (IllegalMonitorStateException e) {
				logger.debug("IllegalMonitorStateException: "+e.getMessage());
			}	
		}
		
		if (!unsubscriptionConfirmed) {
			logger.error("FAILED");
			return false;
		}
		else{
			logger.warn("PASSED");
			return true;
		}
	}
	
	private static boolean registrationTest(String id,boolean multiple){		
		Response response;
		response = client.register(id);
		
		boolean error = response.getClass().equals(ErrorResponse.class);
		
		if ((!error && !multiple) || (error && multiple)) {
			logger.warn("PASSED "+response.toString());
			return true;
		}
			
		logger.error("FAILED "+ response.toString());
		return false;
	}
	
	
	private static boolean requestAccessTokenTest(boolean expired) {
		Response response;
		response = client.requestToken();
		
		if (expired){
			if(response.getClass().equals(ErrorResponse.class)) {
				logger.error("FAILED "+response.toString());
				return false;
			}
			else {
				logger.warn("PASSED "+response.toString());	
				return true;
			}
		}
		else {
			if(response.getClass().equals(ErrorResponse.class)) {
				logger.warn("PASSED "+response.toString());
				return true;
			}
			else {
				logger.error(response.toString());
				return false;
			}
		}
	}
	
	public static void main(String[] args) {
		logger.warn("**********************************************************");
		logger.warn("***     SPARQL 1.1 SE Protocol Service test suite      ***");
		logger.warn("**********************************************************");
		logger.warn("***   WARNING: the RDF store content will be ERASED    ***");
		logger.warn("***         Do you want to continue (yes/no)?          ***");
		logger.warn("**********************************************************");
		Scanner scanner = new Scanner(System.in);
		scanner.useDelimiter("\\n"); //"\\z" means end of input
		String input = scanner.next();
		if (!input.equals("yes")) {
			scanner.close();
			logger.info("Bye bye! :-)");
			System.exit(0);
		}
		logger.warn("**********************************************************");
		logger.warn("***                Are you sure (yes/no)?              ***");
		logger.warn("**********************************************************");
		input = scanner.next();
		if (!input.equals("yes")) {
			scanner.close();
			logger.info("Bye bye! :-)");
			System.exit(0);
		}
		scanner.close();
		
		properties = new SPARQL11SEProperties("client.properties");
		client = new SPARQL11SEProtocol(properties);	
		logger.info("SPARQL 1.1 SE Protocol Service properties: "+client.toString());
		
		// UPDATE
		boolean ret =updateTest("prefix chat:<http://wot.arces.unibo.it/chat#> delete {?s ?p ?o} insert {chat:Sub chat:Pred \"卢卡\"} where {?s ?p ?o}",false,false);
		results.addResult("Update", ret);
		
		// QUERY
		ret = queryTest("select ?o where {?s ?p ?o}","卢卡",false,false);
		results.addResult("Query and UTF8", ret);
		
		// SUBSCRIBE
		ret = subscribeTest("select ?o where {?s ?p ?o}",false,false);
		results.addResult("Subscribe", ret);
		
		//SUBSCRIBE CONFIRM
		ret = waitSubscribeConfirm();
		results.addResult("Subscribe confirmed", ret);
		
		//FIRST NOTIFICATION
		ret = waitNotification();
		results.addResult("First results received", ret);
		
		// PING
		ret = pingTest();
		results.addResult("Ping received", ret);
		
		// TRIGGER A NOTIFICATION
		subscriptionConfirmed = true;
		notificationReceived = false;
		ret = updateTest("prefix chat:<http://wot.arces.unibo.it/chat#> delete {?s ?p ?o} insert {chat:Sub chat:Pred \"ვაიმეე\"} where {?s ?p ?o}",false,false);
		results.addResult("Triggering update", ret);
		
		//WAIT NOTIFICATION
		ret = waitNotification();
		results.addResult("Notification", ret);
		
		// UNSUBSCRIBE
		ret = unsubscribeTest(spuid,false,false);
		results.addResult("Unsubscribe", ret);
			
		// WAIT UNSUBSCRIBE CONFIRM
		ret = waitUnsubscribeConfirm();
		results.addResult("Unsubscribe confirmed", ret);
		
		//**********************
		// Enable security
		//**********************
		logger.info("Switch to secure mode");
		properties = new SPARQL11SEProperties("secureclient.properties");
		client = new SPARQL11SEProtocol(properties);	
		logger.info("SPARQL 1.1 SE Protocol Service properties: "+client.toString());
		
		// REGISTRATION
		String identity = UUID.randomUUID().toString();
		ret = registrationTest(identity,false);
		results.addResult("Registration ", ret);
		
		// REGISTRATION (multiple registration)
		ret = registrationTest(identity,true);
		results.addResult("Multiple registration ", ret); 
		
		// REQUEST ACCESS TOKEN (new);
		ret = requestAccessTokenTest(true);	
		results.addResult("Access token ", ret); 
		
		// REQUEST ACCESS TOKEN (not expired);
		ret = requestAccessTokenTest(false);			
		results.addResult("Token not expired ", ret); 
		
		// REQUEST ACCESS TOKEN (expired);
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep(properties.getExpiringSeconds()+1*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = requestAccessTokenTest(properties.isTokenExpired());	
		results.addResult("Refresh token ", ret); 
		
		// SECURE UPDATE
		if (properties.isTokenExpired()) requestAccessTokenTest(true);
		ret = updateTest("prefix chat:<http://wot.arces.unibo.it/chat#> delete {?s ?p ?o} insert {chat:Sub chat:Pred \"ვაიმეე\"} where {?s ?p ?o}",true,properties.isTokenExpired());
		results.addResult("Secure update ", ret); 
		
		// SECURE UPDATE (expired token)
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep(properties.getExpiringSeconds()+1*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = updateTest("prefix chat:<http://wot.arces.unibo.it/chat#> delete {?s ?p ?o} insert {chat:Sub chat:Pred \"vaimee!\"} where {?s ?p ?o}",true,properties.isTokenExpired());
		results.addResult("Secure update (expired) ", ret); 
		
		// SECURE QUERY
		if (properties.isTokenExpired()) requestAccessTokenTest(true);
		ret = queryTest("select ?o where {?s ?p ?o}","ვაიმეე",true,properties.isTokenExpired());
		results.addResult("Secure query ", ret); 
		
		// SECURE QUERY (expired token)
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep(properties.getExpiringSeconds()+1*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = queryTest("select ?o where {?s ?p ?o}","ვაიმეე",true,properties.isTokenExpired());
		results.addResult("Secure query (expired) ", ret); 
		
		// SECURE SUBSCRIBE
		if (properties.isTokenExpired()) requestAccessTokenTest(true);
		ret = subscribeTest("select ?o where {?s ?p ?o}",true,properties.isTokenExpired());
		results.addResult("Secure subscribe ", ret); 
		
		// SECURE SUBSCRIBE (token expired)
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep(properties.getExpiringSeconds()+1*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = subscribeTest("select ?o where {?s ?p ?o}",true,properties.isTokenExpired());
		results.addResult("Secure subscribe (expired) ", ret); 
		
		//WAITING FIRST PING
		ret = pingTest();
		results.addResult("Secure ping ", ret); 
		
		// TRIGGER A NOTIFICATION 
		if (properties.isTokenExpired()) requestAccessTokenTest(properties.isTokenExpired());
		else {
			logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
			try {
				Thread.sleep(properties.getExpiringSeconds()+1*1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}
		requestAccessTokenTest(properties.isTokenExpired());
		ret = updateTest("prefix chat:<http://wot.arces.unibo.it/chat#> delete {?s ?p ?o} insert {chat:Sub chat:Pred \"卢卡\"} where {?s ?p ?o}",true,properties.isTokenExpired());
		results.addResult("Secure triggering update ", ret); 
		
		// UNSUBSCRIBE (expired token)
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep(properties.getExpiringSeconds()+1*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = unsubscribeTest(spuid,true,properties.isTokenExpired());
		results.addResult("Secure unsubscribe (expired) ", ret); 
		
		// UNSUBSCRIBE
		if (properties.isTokenExpired()) requestAccessTokenTest(true);
		ret = unsubscribeTest(spuid,true,properties.isTokenExpired());
		results.addResult("Secure unsubscribe ", ret); 		
		
		results.print();
	}
}
