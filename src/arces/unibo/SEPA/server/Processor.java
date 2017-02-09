/* This class implements the processing of the requests coming form the scheduler
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

package arces.unibo.SEPA.server;

import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;
import arces.unibo.SEPA.commons.QueryRequest;
import arces.unibo.SEPA.commons.Response;
import arces.unibo.SEPA.commons.SubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeRequest;
import arces.unibo.SEPA.commons.UnsubscribeResponse;
import arces.unibo.SEPA.commons.UpdateRequest;
import arces.unibo.SEPA.commons.UpdateResponse;

public class Processor extends Observable implements Observer {
	private static String tag = "Processor";
	
	private QueryProcessor queryProcessor;
	private UpdateProcessor updateProcessor;	
	private SPUManager spuManager;
	private Endpoint endpoint;
	
	public Processor(Properties properties) {	
		//Create SPARQL 1.1 interface
		endpoint = new Endpoint(properties);
		
		//Create processor to manage (optimize) QUERY and UPDATE request
		queryProcessor = new QueryProcessor(endpoint);
		updateProcessor = new UpdateProcessor(endpoint);
				
		//Subscriptions manager
		spuManager = new SPUManager(endpoint);
		spuManager.addObserver(this);
	}
	
	public void processQuery(QueryRequest req) {
		Logger.log(VERBOSITY.DEBUG, tag, "QUERY #"+req.getToken());
		Response res = queryProcessor.process(req);
		
		//Send response back
		Logger.log(VERBOSITY.DEBUG, tag, "QUERY response #"+res.getToken());
		setChanged();
		notifyObservers(res);
	}
	
	public void processUpdate(UpdateRequest req) {
		Logger.log(VERBOSITY.DEBUG, tag, "UPDATE #"+req.getToken());
		Response res = updateProcessor.process(req);
		
		//Send response back
		Logger.log(VERBOSITY.DEBUG, tag, "UPDATE response #"+res.getToken());
		setChanged();
		notifyObservers(res);
				
		//Subscriptions processing
		Logger.log(VERBOSITY.DEBUG, tag, "*** PROCESS SUBSCRIPTIONS ***");
		if (UpdateResponse.class.equals(res.getClass())) spuManager.processUpdate((UpdateResponse)res);	
	}
	
	public void processSubscribe(SubscribeRequest req){
		Logger.log(VERBOSITY.DEBUG, tag, "SUBSCRIBE #"+req.getToken());
		spuManager.processSubscribe(req);
	}
	
	public void processUnsubscribe(UnsubscribeRequest req) {
		Logger.log(VERBOSITY.DEBUG, tag, "UNSUBSCRIBE #"+req.getToken());
		String spuid = spuManager.processUnsubscribe(req);
		UnsubscribeResponse res = new UnsubscribeResponse(req.getToken(),spuid);
		
		//Send response back
		Logger.log(VERBOSITY.DEBUG, tag, "UNSUBSCRIBE response #"+res.getToken());
		setChanged();
		notifyObservers(res);
	}

	@Override
	public void update(Observable o, Object arg) {
		Logger.log(VERBOSITY.DEBUG, tag, "<< SPU MANAGER notification ");
		setChanged();
		notifyObservers(arg);
	}
}
