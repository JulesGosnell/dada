/**
 * 
 */
package org.omo.core;

public interface Table<V> {
	V get(int key);
	V put(int key, V value);
	V rem(int key, V value);
}