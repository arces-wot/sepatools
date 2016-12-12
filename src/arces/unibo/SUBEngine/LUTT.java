package arces.unibo.SUBEngine;

/**
 * This class represents the LUTT (LookUp Triples Table)
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class LUTT {
	public LUTT(SubscribeRequest request) {}
	
	/**
	 * The method performs the LUTT matching on the input triples
	 *
	 * @param  triples a set of added and removed RDF triples
	 * @return the subset of added and removed RDF triples that match the LUTT. 
	 * The method returns null if no triple matches the LUTT
	 * 
	 * @see		ARTriples
	 */
	public ARTriples matching(ARTriples triples) {
		//TODO to be implemented
		if (triples == null) return null;
		
		return new ARTriples(null,null);
	}
}
