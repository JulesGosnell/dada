package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class MapModelView<K, V> extends AbstractModel<K, V> implements View<K, V> {

	public interface Adaptor<K, V> {
		public K getKey(V value);
	}
	
	protected final Adaptor<K, V> adaptor;
	protected final Map<K, V> map = new HashMap<K, V>();
	
	public MapModelView(String name, Metadata<K, V> metadata, Adaptor<K, V> adaptor) {
		super(name, metadata);
		this.adaptor =  adaptor;
	}
	
	// Model
	@Override
	protected Collection<V> getData() {
		return new ArrayList<V>(map.values());
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	@Override
	public void insert(V value) {
		synchronized (map.values()) {
			map.put(adaptor.getKey(value), value);
		}
		notifyInsertion(value);
	}

	@Override
	public void update(V oldValue, V newValue) {
		synchronized (map.values()) {
			map.put(adaptor.getKey(newValue), newValue);
		}
		notifyUpdate(newValue);
	}

	// View
	@Override
	public void delete(K key) {
		synchronized (map.values()) {
			map.remove(key);
		}
		
	}

	@Override
	public void batch(Collection<V> insertions, Collection<Update<V>> updates, Collection<K> deletions) {
		synchronized (map.values()) {
			for (V insertion : insertions)
				map.put(adaptor.getKey(insertion), insertion);
		}
		notifyBatch(insertions, null, null);
		// TODO: extend
	}

}
