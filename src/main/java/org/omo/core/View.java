/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<K, V> {
	 
	void batch(Collection<V> insertions, Collection<V> updates, Collection<K> deletions);
	
	void insert(V value);
	void update(V oldValue, V newValue);
	void delete(K key);
	
}