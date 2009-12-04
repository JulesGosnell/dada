package org.omo.core;

import java.util.Collection;

public interface Range<V> {

	V random();
	V getMin();
	V getMax();
	
	// these are only implemented by some ranges - ugly
	Collection<V> getValues();
	int size();
}
