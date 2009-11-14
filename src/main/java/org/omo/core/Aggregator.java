package org.omo.core;

import java.util.Collection;

public interface Aggregator<A, V> {

	A getAggregate();
	
	void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions);

}