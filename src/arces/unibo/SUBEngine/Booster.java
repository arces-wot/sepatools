package arces.unibo.SUBEngine;

import java.util.Set;

/**
 * This class represents the BOOSTER component of the SUB Engine
 * 
 * The BOOSTER implements the subscription algorithm
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class Booster {

	private SubscribeRequest query;
	private CTS cts;
	
	private class MatchingResult {
		public SPARQLBindingsResults matchingBindings;
		public SubscribeRequest reducedQuery;
	}
	
	public Booster(CTS cts,SubscribeRequest query){
		this.cts = cts;
		this.query = query;
	}
	
	public ARBindingsResults run(ARTriples triples) {
		SPARQLBindingsResults added = findBindingsResults(triples.getAdded());
		cts.update(triples);
		SPARQLBindingsResults removed = findBindingsResults(triples.getRemoved());
		return new ARBindingsResults(added,removed);
	}
	
	private SPARQLBindingsResults findBindingsResults(Set<Triple> triples){
		SPARQLBindingsResults ret = new SPARQLBindingsResults();
		for (Triple triple: triples) {
			MatchingResult matchingResult = match(query,triple);
			SPARQLBindingsResults queryResult = cts.query(matchingResult.reducedQuery);
			ret.merge(queryResult, matchingResult.matchingBindings);
		}
		return ret;
	}
	
	private MatchingResult match(SubscribeRequest query,Triple t) {
		//TODO to be implemented
		MatchingResult ret = new MatchingResult();
		
		return ret;
	}
	
}
