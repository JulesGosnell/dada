/**
 * 
 */
package org.omo.core;

public interface PartitionMap<K, V> {
	K getKey(V value);
	boolean containsKey(K key);
	View<K, V> getPartition(K key);
	int size();
}