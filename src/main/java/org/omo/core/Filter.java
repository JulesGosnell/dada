/**
 * 
 */
package org.omo.core;

public interface Filter<V> {
	
	boolean apply(V value);
	
}