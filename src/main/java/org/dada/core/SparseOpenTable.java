package org.omo.core;

import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If asked for a value that is not present create one using given factory, add it to our map and return
 * it - all in a thread-safe manner.
 * 
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public class SparseOpenTable<K, V> implements Table<K, V> {
	
	private static final Logger LOG = LoggerFactory.getLogger(SparseOpenTable.class);

	public static interface Factory<K, V> {
		public V create(K key, ConcurrentMap<K, V> map) throws Exception;
	}
	
	private final SparseOpenTable.Factory<K, V> factory;
	private final ConcurrentMap<K, V> map;
	
	public SparseOpenTable(ConcurrentMap<K, V> map, SparseOpenTable.Factory<K, V> factory) {
		this.factory = factory;
		this.map = map;
	}
	
	
	public V get(K key) {
		V value = map.get(key);
		if (value == null) {
			// pay careful attention here - plenty of scope for error...
			V newValue = null;
			try {
				newValue = factory.create(key, map);
			} catch (Exception e) {
				LOG.error("unable to create new Table item", e);
			}
			V oldValue = map.putIfAbsent(key, newValue);
			value = oldValue == null ? newValue : oldValue;
			// N.B. newValue may lose race and be thrown away - so be careful 
		}
		return value;
	}
	
	public V put(K key, V value) {
		return map.put(key, value);
	}

	public V rem(K key, V value) {
		boolean removed = map.remove(key, value);
		return removed ? value : null;
		
	}
}