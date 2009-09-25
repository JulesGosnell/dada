/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;

public interface Filter<V> {
	
	boolean apply(V value);
	LinkedList<V> apply(Collection<V> values);
	
}