/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<K, V> {
	 
	void insert(V value);
	void update(V oldValue, V newValue);
	void delete(K key);

	void batch(Collection<V> insertions, Collection<Update<V>> updates, Collection<K> deletions);
	
}