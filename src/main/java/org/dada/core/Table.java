/**
 * 
 */
package org.omo.core;

public interface Table<K, V> {
	V get(K key);
	V put(K key, V value);
	V rem(K key, V value);
}