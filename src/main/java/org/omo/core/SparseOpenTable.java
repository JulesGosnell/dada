package org.omo.core;

import java.util.concurrent.ConcurrentMap;

/**
 * If asked for a value that is not present create one using given factory, add it to our map and return
 * it - all in a thread-safe manner.
 * 
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public class SparseOpenTable<V> implements Table<V> {
	
	public static interface Factory<V> {
		public V create(Integer key, ConcurrentMap<Integer, V> map);
	}
	
	private final SparseOpenTable.Factory<V> factory;
	private final ConcurrentMap<Integer, V> map;
	
	public SparseOpenTable(ConcurrentMap<Integer, V> map, SparseOpenTable.Factory<V> factory) {
		this.factory = factory;
		this.map = map;
	}
	
	
	public V get(int key) {
		V value = map.get(key);
		if (value == null) {
			// pay careful attention here - plenty of scope for error...
			V newValue = factory.create(key, map);
			V oldValue = map.putIfAbsent(key, newValue);
			value = oldValue == null ? newValue : oldValue;
			// N.B. newValue may lose race and be thrown away - so be careful 
		}
		return value;
	}
	
	public V put(int key, V value) {
		return map.put(key, value);
	}

	public V rem(int key, V value) {
		boolean removed = map.remove(key, value);
		return removed ? value : null;
		
	}
}