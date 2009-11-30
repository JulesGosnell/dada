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
public class SparseTable<V> implements Table<V> {
	
	public static interface Factory<V> {
		public V create(Integer key, ConcurrentMap<Integer, V> map);
	}
	
	private final SparseTable.Factory<V> factory;
	private final ConcurrentMap<Integer, V> map;
	
	public SparseTable(ConcurrentMap<Integer, V> map, SparseTable.Factory<V> factory) {
		this.factory = factory;
		this.map = map;
	}
	
	
	public V get(Integer key) {
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
	
	public V put(Integer key, V value) {
		return map.put(key, value);
	}

	public V rem(Integer key, V value) {
		boolean removed = map.remove(key, value);
		return removed ? value : null;
		
	}
}