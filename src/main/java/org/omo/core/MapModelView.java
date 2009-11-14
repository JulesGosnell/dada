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

	// View
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		Collection<V> insertionsOut = new ArrayList<V>();
		synchronized (map.values()) {
			for (Update<V> insertion : insertions) {
				V newValue = insertion.getNewValue();
				map.put(adaptor.getKey(newValue), newValue);
				insertionsOut.add(newValue);
			}
		}
		notifyUpdates(insertionsOut);
		// TODO: extend
	}

}
