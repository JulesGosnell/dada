package org.omo.core;


public interface Aggregator<A, K, V> extends View<K, V>{

	A getAggregate();

}