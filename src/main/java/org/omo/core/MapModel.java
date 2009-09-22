package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapModel<Key, Value> extends AbstractModel<Key, Value> implements View<Key, Value> {

	public interface Adaptor<Key, Value> {
		public Key getKey(Value value);
	}
	
	protected final Adaptor<Key, Value> adaptor;
	protected final Map<Key, Value> map = new HashMap<Key, Value>();
	
	public MapModel(String name, Metadata<Key, Value> metadata, Adaptor<Key, Value> adaptor) {
		super(name, metadata);
		this.adaptor =  adaptor;
	}
	
	// Model
	@Override
	protected Collection<Value> getData() {
		return new ArrayList<Value>(map.values());
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
	public void insert(Value value) {
		synchronized (map.values()) {
			map.put(adaptor.getKey(value), value);
		}
		notifyInsertion(value);
	}

	@Override
	public void update(Value value) {
		synchronized (map.values()) {
			map.put(adaptor.getKey(value), value);
		}
		notifyUpdate(value);
	}

	// View
	@Override
	public void delete(Key key) {
		synchronized (map.values()) {
			map.remove(key);
		}
		
	}

	@Override
	public void batch(Collection<Value> insertions, Collection<Value> updates, Collection<Key> deletions) {
		synchronized (map.values()) {
			for (Value insertion : insertions)
				map.put(adaptor.getKey(insertion), insertion);
		}
		notifyBatch(insertions, null, null);
		// TODO: extend
	}

}
