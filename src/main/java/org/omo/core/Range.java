package org.omo.core;

import java.util.Collection;

public interface Range<V> {

	boolean contains(V value);
	V random();
	Collection<V> getValues();
	int size();
	V getMin();
	V getMax();
	
}
