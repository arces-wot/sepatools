/* This program can be used and extended to test a SEPA implementation and API

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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
	protected static final Logger logger = LogManager.getLogger("SEPATest");
	protected static Results results = new SEPATest().new Results();
	protected static TestNotificationHandler handler = new TestNotificationHandler();
	
	//Subscription variables
	protected static boolean subscribeConfirmReceived = false;
	protected static boolean unsubscriptionConfirmReceived = false;
	protected static String spuid = null;
	protected static boolean pingReceived = false;
	protected static boolean notificationReceived = false;
	protected static Object sync = new Object();
	
	protected static SPARQL11SEProtocol client;
	protected static SPARQL11SEProperties properties;
	
	protected static final long subscribeConfirmDelay = 2000;
	protected static final long pingDelay = 5000;
	
	protected class Results {
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
	
	protected class Result {
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
	
	protected static class TestNotificationHandler implements NotificationHandler {

		@Override
		public void semanticEvent(Notification notify) {
			synchronized(sync) {
				logger.debug(notify.toString());
				notificationReceived = true;
				sync.notify();
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
				subscribeConfirmReceived = true;
				sync.notify();
			}
		}

		@Override
		public void unsubscribeConfirmed(UnsubscribeResponse response) {
			synchronized(sync) {
				logger.debug(response.toString());
				unsubscriptionConfirmReceived = true;
				sync.notify();
			}
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			synchronized(sync) {
				logger.debug(errorResponse.toString());	
				sync.notify();
			}
		}
	}
	
	protected static boolean updateTest(String sparql,boolean secure) {
		notificationReceived = false;
		
		UpdateRequest update = new UpdateRequest(sparql);
		
		if (!secure) logger.info(update.toString());
		else logger.info("SECURE "+update.toString());
		
		Response response;
		if (secure) response = client.secureUpdate(update);
		else response = client.update(update);
		
		logger.debug(response.toString());
		
		return !response.getClass().equals(ErrorResponse.class);
	}
	
	protected static boolean queryTest(String sparql,String utf8,boolean secure) {
		QueryRequest query = new QueryRequest(sparql);
		
		if (!secure) logger.info(query.toString());
		else logger.info("SECURE "+query.toString());
				
		Response response;
		if (!secure) response = client.query(query);
		else response = client.secureQuery(query);
		
		logger.debug(response.toString());
		
		boolean error = response.getClass().equals(ErrorResponse.class);
			
		if (!error && utf8 != null) {
			QueryResponse queryResponse = (QueryResponse) response;
			List<Bindings> results = queryResponse.getBindingsResults().getBindings();
			if (results.size() == 1) {
				Bindings bindings = results.get(0);
				if(bindings.isLiteral("o")) {
					String value = bindings.getBindingValue("o");
					if (value.equals(utf8)) return true;
				}
			}
			
			return false;
		}
		
		return !error;
	}
	
	protected static boolean subscribeTest(String sparql,boolean secure) {
		subscribeConfirmReceived = false;
		notificationReceived = false;
		
		SubscribeRequest sub = new SubscribeRequest(sparql);

		if (secure) logger.info("SECURE "+sub.toString());
		else logger.info(sub.toString());

		Response response;
		
		if (!secure) response = client.subscribe(sub, handler);
		else response = client.secureSubscribe(sub, handler);
		
		logger.debug(response.toString());
		
		return !response.getClass().equals(ErrorResponse.class);
	}
	
	protected static boolean waitSubscribeConfirm(){	
		synchronized(sync) {
			if (subscribeConfirmReceived) return true;
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
		
		return (subscribeConfirmReceived);
	}
	
	protected static boolean waitPing() {		
		long delay = pingDelay+(pingDelay/2);
		synchronized(sync) {
			pingReceived = false;
			try {
				logger.debug("Waiting ping in "+delay+" ms...");
				sync.wait(delay);
			} catch (InterruptedException e) {
				logger.info(e.getMessage());
			}	
		}	
		
		return pingReceived;
	}
			
	protected static boolean waitNotification() {	
		synchronized(sync) {
			if (notificationReceived) return true;
			try {
				sync.wait(subscribeConfirmDelay);
			} catch (InterruptedException e) {
				logger.info(e.getMessage());
			}
		}	
		
		return notificationReceived;
	}
	
	protected static boolean unsubscribeTest(String spuid,boolean secure) {	
		unsubscriptionConfirmReceived = false;
		
		UnsubscribeRequest unsub = new UnsubscribeRequest(spuid);

		Response response;
		if (!secure) response = client.unsubscribe(unsub);
		else response = client.secureUnsubscribe(unsub);
		
		logger.debug(response.toString());
		
		return !response.getClass().equals(ErrorResponse.class);
	}
	
	protected static boolean waitUnsubscribeConfirm() {
		synchronized(sync) {
			if (unsubscriptionConfirmReceived) return true;
			try {
				sync.wait(5000);
			} catch (InterruptedException e) {
				logger.debug("InterruptedException: "+e.getMessage());
			}				
		}
		
		return unsubscriptionConfirmReceived;
	}
	
	protected static boolean registrationTest(String id){		
		Response response;
		response = client.register(id);		
		return !response.getClass().equals(ErrorResponse.class);
	}
		
	protected static boolean requestAccessTokenTest() {
		Response response;
		response = client.requestToken();
		
		logger.debug(response.toString());
		
		return !response.getClass().equals(ErrorResponse.class);
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
		
		properties = new SPARQL11SEProperties("client.json");
		if (!properties.loaded()) {
			logger.fatal("Properties file is null");
			System.exit(-1);
		}
		client = new SPARQL11SEProtocol(properties);	
		logger.info("SPARQL 1.1 SE Protocol Service properties: "+client.toString());
		
		// UPDATE
		boolean ret =updateTest("prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"測試\"} where {?s ?p ?o}",false);
		results.addResult("Update", ret);
		if (ret) logger.warn("Update PASSED");
		else logger.error("Update FAILED");
		
		// QUERY
		ret = queryTest("select ?o where {?s ?p ?o}","測試",false);
		results.addResult("Query", ret);
		if (ret) logger.warn("Query PASSED");
		else logger.error("Query FAILED");
		
		// SUBSCRIBE
		ret = subscribeTest("select ?o where {?s ?p ?o}",false);
		results.addResult("Subscribe - request", ret);
		if (ret) logger.warn("Subscribe PASSED");
		else logger.error("Subscribe FAILED");
		
		//SUBSCRIBE CONFIRM
		ret = waitSubscribeConfirm();
		results.addResult("Subscribe - confirm", ret);
		if (ret) logger.warn("Subscribe confirmed PASSED");
		else logger.error("Subscribe confirmed FAILED");
		
		//FIRST NOTIFICATION
		ret = waitNotification();
		results.addResult("Subscribe - results", ret);
		if (ret) logger.warn("First results received PASSED");
		else logger.error("First results received FAILED");
		
		// PING
		ret = waitPing();
		results.addResult("Subscribe - ping", ret);
		if (ret) logger.warn("Ping received PASSED");
		else logger.error("Ping recevied FAILED");
		
		// TRIGGER A NOTIFICATION
		ret = updateTest("prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"ვაიმეე\"} where {?s ?p ?o}",false);
		results.addResult("Subscribe - triggering", ret);
		if (ret) logger.warn("Triggering update PASSED");
		else logger.error("Triggering update FAILED");
		
		//WAIT NOTIFICATION
		ret = waitNotification();
		results.addResult("Subscribe - notification", ret);
		if (ret) logger.warn("Notification PASSED");
		else logger.error("Notification FAILED");
		
		// UNSUBSCRIBE
		ret = unsubscribeTest(spuid,false);
		results.addResult("Unsubscribe - request", ret);
		if (ret) logger.warn("Unsubscribe PASSED");
		else logger.error("Unsubscribe FAILED");
		
		// WAIT UNSUBSCRIBE CONFIRM
		ret = waitUnsubscribeConfirm();
		results.addResult("Unsubscribe - confirm", ret);
		if (ret) logger.warn("Unsubscribe confirmed PASSED");
		else logger.error("Unsubscribe confirmed FAILED");
		
		// PING
		ret = !waitPing();
		results.addResult("Unsubscribe - ping", ret);
		if (ret) logger.warn("Ping not received PASSED");
		else logger.error("Ping not recevied FAILED");
		
		//**********************
		// Enable security
		//**********************
		logger.info("Switch to secure mode");
		properties = new SPARQL11SEProperties("secureclient.json");
		client = new SPARQL11SEProtocol(properties);	
		logger.info("SPARQL 1.1 SE Protocol Service properties: "+client.toString());
		
		// REGISTRATION
		String identity = UUID.randomUUID().toString();
		ret = registrationTest(identity);
		results.addResult("Registration", ret);
		if (ret) logger.warn("Registration PASSED");
		else logger.error("Registration FAILED");
		
		// REGISTRATION (multiple registration)
		ret = !registrationTest(identity);
		results.addResult("Registration not allowed", ret); 
		if (ret) logger.warn("Registration (not allowed) PASSED");
		else logger.error("Registration (not allowed) FAILED");
		
		// REQUEST ACCESS TOKEN
		ret = requestAccessTokenTest();	
		results.addResult("Access token", ret); 
		if (ret) logger.warn("Access token PASSED");
		else logger.error("Access token FAILED");
		
		// REQUEST ACCESS TOKEN (not expired);
		if (!properties.isTokenExpired()) ret = !requestAccessTokenTest();			
		else ret = false;
		results.addResult("Access token not expired", ret); 
		if (ret) logger.warn("Access token (not expired) PASSED");
		else logger.error("Access token (not expired) FAILED");
		
		// REQUEST ACCESS TOKEN (expired);
		logger.info("Waiting token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds()+1)*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		if (properties.isTokenExpired()) ret = requestAccessTokenTest();	
		else ret = false;
		results.addResult("Access token expired", ret); 
		if (ret) logger.warn("Access token (expired) PASSED");
		else logger.error("Access token (expired) FAILED");
		
		// SECURE UPDATE
		if (properties.isTokenExpired()) requestAccessTokenTest();
		ret = updateTest("prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"ვაიმეე\"} where {?s ?p ?o}",true);
		results.addResult("Secure update ", ret); 
		if (ret) logger.warn("Secure update PASSED");
		else logger.error("Secure update FAILED");
		
		// SECURE UPDATE (expired token)
		logger.info("Waiting token expiring in "+properties.getExpiringSeconds()+" + 1 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds()+1)*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = !updateTest("prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"vaimee!\"} where {?s ?p ?o}",true);
		results.addResult("Secure update (expired)", ret); 
		if (ret) logger.warn("Secure update (expired) PASSED");
		else logger.error("Secure update (expired) FAILED");
		
		// SECURE QUERY
		if (properties.isTokenExpired()) requestAccessTokenTest();
		ret = queryTest("select ?o where {?s ?p ?o}","ვაიმეე",true);
		results.addResult("Secure query", ret); 
		if (ret) logger.warn("Secure query PASSED");
		else logger.error("Secure query FAILED");
		
		// SECURE QUERY (expired token)
		logger.info("Waiting token expiring in "+properties.getExpiringSeconds()+" + 1 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds()+1)*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = !queryTest("select ?o where {?s ?p ?o}","ვაიმეე",true);
		results.addResult("Secure query (expired)", ret); 
		if (ret) logger.warn("Secure query (expired) PASSED");
		else logger.error("Secure query (expired) FAILED");
		
		// SECURE SUBSCRIBE
		if (properties.isTokenExpired()) requestAccessTokenTest();
		ret = subscribeTest("select ?o where {?s ?p ?o}",true);
		results.addResult("Secure subscribe - request", ret); 
		if (ret) logger.warn("Secure subscribe PASSED");
		else logger.error("Secure subscribe FAILED");
		
		//SUBSCRIBE CONFIRM
		ret = waitSubscribeConfirm();
		results.addResult("Secure subscribe - confirm", ret);
		if (ret) logger.warn("Secure subscribe confirmed PASSED");
		else logger.error("Secure subscribe confirmed FAILED");
		
		//FIRST NOTIFICATION
		ret = waitNotification();
		results.addResult("Secure subscribe - results", ret);
		if (ret) logger.warn("First results received PASSED");
		else logger.error("First results received FAILED");
		
		// PING
		ret = waitPing();
		results.addResult("Secure subscribe - ping", ret);
		if (ret) logger.warn("Secure ping received PASSED");
		else logger.error("Secure ping recevied FAILED");
			
		// TRIGGER A NOTIFICATION 
		if (properties.isTokenExpired()) requestAccessTokenTest();
		ret = updateTest("prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"卢卡\"} where {?s ?p ?o}",true);
		results.addResult("Secure subscribe - triggering", ret); 
		if (ret) logger.warn("Secure triggering update PASSED");
		else logger.error("Secure triggering update FAILED");
		
		//NOTIFICATION
		ret = waitNotification();
		results.addResult("Secure subscribe - notification", ret);
		if (ret) logger.warn("Secure subscribe - notification PASSED");
		else logger.error("Secure subscribe - notification FAILED");
			
		// UNSUBSCRIBE (expired)
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds()+1)*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = unsubscribeTest(spuid,true);
		results.addResult("Secure unsubscribe (expired) - request", ret); 
		if (ret) logger.warn("Secure unsubscribe - expired PASSED");
		else logger.error("Secure unsubscribe - expired FAILED");
					
		// WAIT UNSUBSCRIBE CONFIRM
		ret = !waitUnsubscribeConfirm();
		results.addResult("Secure unsubscribe (expired) - confirm", ret);
		if (ret) logger.warn("Secure unsubscribe confirmed PASSED");
		else logger.error("Secure unsubscribe confirmed FAILED");
					
		// UNSUBSCRIBE
		if (properties.isTokenExpired()) requestAccessTokenTest();
		ret = unsubscribeTest(spuid,true);
		results.addResult("Secure unsubscribe - request", ret); 		
		if (ret) logger.warn("Secure unsubscribe PASSED");
		else logger.error("Secure unsubscribe FAILED");
			
		// WAIT UNSUBSCRIBE CONFIRM
		ret = waitUnsubscribeConfirm();
		results.addResult("Secure unsubscribe - confirm", ret);
		if (ret) logger.warn("Unsubscribe confirmed PASSED");
		else logger.error("Unsubscribe confirmed FAILED");
					
		//WAITING PING
		ret = !waitPing();
		results.addResult("Secure unsubscribe - ping", ret); 		
		if (ret) logger.warn("Secure unsubscribe - ping PASSED");
		else logger.error("Secure unsubscribe - ping FAILED");
		
		// SECURE SUBSCRIBE
		logger.info("Wait token expiring in "+properties.getExpiringSeconds()+" +1 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds()+1)*1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		ret = subscribeTest("select ?o where {?s ?p ?o}",true);
		results.addResult("Secure subscribe (expired) - request", ret); 
		if (ret) logger.warn("Secure subscribe PASSED");
		else logger.error("Secure subscribe FAILED");
				
		//SUBSCRIBE CONFIRM
		ret = !waitSubscribeConfirm();
		results.addResult("Secure subscribe (expired) - confirm", ret);
		if (ret) logger.warn("Secure subscribe confirmed PASSED");
		else logger.error("Secure subscribe confirmed FAILED");
				
		results.print();
	}
}
