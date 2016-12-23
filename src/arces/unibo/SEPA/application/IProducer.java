package arces.unibo.SEPA.application;

import arces.unibo.SEPA.commons.SPARQLQuerySolution;

public interface IProducer extends IClient {
	 public boolean update(SPARQLQuerySolution forcedBindings);
}
