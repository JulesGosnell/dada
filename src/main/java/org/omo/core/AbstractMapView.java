package org.omo.core;

import java.util.Collection;
import java.util.Map;


public class AbstractMapView<K, V> extends AbstractView<K, V> {

	protected final Map<K, V> map;
	
	public AbstractMapView(Map<K, V> map) {
		this.map = map;
	}
	
	@Override
	protected Object getLock() {
		return map;
	}

	// View
	
	@Override
	public void insert(V value) {
		throw new UnsupportedOperationException("NYI");
	}
	
	@Override
	public void update(V oldValue, V newValue) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(K key) {
		synchronized (map) {
			map.remove(key);
		}
	}

	@Override
	public void batch(Collection<V> insertions, Collection<Update<V>> updates, Collection<K> deletions) {
		throw new UnsupportedOperationException("NYI");
	}


}
