package org.omo.core;

import java.util.Collection;

public interface Aggregator<A, V> {

	A getAggregate();
	
	void insert(Collection<Update<V>> value);

	void update(V oldValue, V newValue);

	void remove(Collection<Update<V>> value);

}