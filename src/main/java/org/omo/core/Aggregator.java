package org.omo.core;

import java.util.Collection;

public interface Aggregator<A, V> {

	A getAggregate();
	
	void insert(Collection<Update<V>> insertions);

	void update(Collection<Update<V>> updates);

	void remove(Collection<Update<V>> deletions);

}