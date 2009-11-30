/**
 * 
 */
package org.omo.core;

public interface Table<V> {
	V get(Integer key);
	V put(Integer key, V value);
	V rem(Integer key, V value);
}