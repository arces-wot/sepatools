/*  This class implements the processing of the requests coming form the scheduler
    Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.processing;

import java.util.Observable;
import java.util.Observer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import arces.unibo.SEPA.client.api.SPARQL11Properties;
import arces.unibo.SEPA.client.api.SPARQL11Protocol;

import arces.unibo.SEPA.commons.request.QueryRequest;
import arces.unibo.SEPA.commons.request.SubscribeRequest;
import arces.unibo.SEPA.commons.request.UnsubscribeRequest;
import arces.unibo.SEPA.commons.request.UpdateRequest;

import arces.unibo.SEPA.commons.response.Response;
import arces.unibo.SEPA.commons.response.UnsubscribeResponse;
import arces.unibo.SEPA.commons.response.UpdateResponse;

public class Processor extends Observable implements Observer {
	
	private QueryProcessor queryProcessor;
	private UpdateProcessor updateProcessor;	
	private SPUManager spuManager;
	private SPARQL11Protocol endpoint;
	private static final Logger logger = LogManager.getLogger("Processor");
	
	public Processor(SPARQL11Properties properties) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {	
		//Create SPARQL 1.1 interface
		endpoint = new SPARQL11Protocol(properties);
		logger.info(endpoint.toString());
		
		//Create processor to manage (optimize) QUERY and UPDATE request
		queryProcessor = new QueryProcessor(endpoint);
		updateProcessor = new UpdateProcessor(endpoint);
				
		//Subscriptions manager
		spuManager = new SPUManager(endpoint);
		spuManager.addObserver(this);
	}
	
	public void processQuery(QueryRequest req) {
		logger.debug("*Process* "+req.toString());
		Response res = queryProcessor.process(req);
		
		//Send response back
		logger.debug("<< "+res.toString());
		setChanged();
		notifyObservers(res);
	}
	
	public void processUpdate(UpdateRequest req) {
		logger.debug("*Process* "+req.toString());
		Response res = updateProcessor.process(req);
		
		//Send response back
		logger.debug("<< "+res.toString());
		setChanged();
		notifyObservers(res);
				
		//Subscriptions processing
		logger.debug("*** Process subscriptions ***");
		if (res.getClass().equals(UpdateResponse.class)) spuManager.processUpdate((UpdateResponse)res);	
	}
	
	public void processSubscribe(SubscribeRequest req){
		logger.debug("*Process* "+req.toString());
		spuManager.processSubscribe(req);
	}
	
	public void processUnsubscribe(UnsubscribeRequest req) {
		logger.debug("*Process* "+req.toString());
		String spuid = spuManager.processUnsubscribe(req);
		UnsubscribeResponse res = new UnsubscribeResponse(req.getToken(),spuid);
		
		//Send response back
		logger.debug("<< "+res.toString());
		setChanged();
		notifyObservers(res);
	}

	@Override
	public void update(Observable o, Object arg) {
		logger.debug("<< SPU MANAGER notification ");
		setChanged();
		notifyObservers(arg);
	}
}
