package org.omo.core;

import java.util.Collection;
import java.util.List;

/**
 * If asked for a value that is not present create one using given factory, add it to our map and return
 * it - all in a thread-safe manner.
 * 
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public class CompactOpenTable<V> implements Table<V> {
	
	public static interface Factory<V> {
		public V create(Integer key, Collection<V> views);
	}
	
	private final CompactOpenTable.Factory<V> factory;
	private final List<V> values;
	
	public CompactOpenTable(List<V> values, CompactOpenTable.Factory<V> factory) {
		this.factory = factory;
		this.values = values;
	}
	
	
	public V get(int key) {
		V value = values.get(key);
		if (value == null) {
			// pay careful attention here - plenty of scope for error...
			throw new UnsupportedOperationException("NYI");
		}
		return value;
	}
	
	public V put(int key, V value) {
		return values.set(key, value);
	}

	public V rem(int key, V value) {
		values.remove(key);
		throw new UnsupportedOperationException("NYI");
		// TODO: check...
		//return value;
	}
}
