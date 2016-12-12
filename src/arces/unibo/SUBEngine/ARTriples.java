package arces.unibo.SUBEngine;

import java.util.Set;

/**
 * This class represents the set of added and removed triples by a SPARQL 1.1 Update
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class ARTriples {
	private Set<Triple> added;
	private Set<Triple> removed;
	
	public ARTriples(Set<Triple> added,Set<Triple> removed) {
		this.added = added;
		this.removed = removed;
	}
	
	public boolean isEmpty() {
		if (added == null && removed == null) return true;
		if (added == null) return removed.isEmpty();
		if (removed == null) return added.isEmpty();
		return added.isEmpty() && removed.isEmpty();
	}

	public void clear() {
		if (added != null) added.clear();
		if (removed != null) removed.clear();	
	}
	
	public Set<Triple> getAdded() {
		return added;
	}

	public Set<Triple> getRemoved() {
		return removed;
	}
	
}
