package org.omo.core;

public interface Aggregator<A, V> {

	A getAggregate();
	
	void insert(V value);

	void update(V oldValue, V newValue);

	void remove(V value);

}