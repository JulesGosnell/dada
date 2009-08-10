package com.nomura.cash;

public interface Aggregator<A, T extends Identifiable> extends Listener<T> {

	A getAggregate();
	
}
