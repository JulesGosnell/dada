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
	
	public MapModel(String name, Adaptor<Key, Value> adaptor) {
		super(name);
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
			for (Value upsertion : updates)
				map.put(adaptor.getKey(upsertion), upsertion);
		}
		notifyUpsertion(updates);
		// TODO: extend
	}

	@Override
	public void update(Value value) {
		synchronized (map.values()) {
			map.put(adaptor.getKey(value), value);
		}
		notifyUpsertion(value);
	}

	@Override
	public void insert(Value value) {
		// TODO Auto-generated method stub
		
	}

}
