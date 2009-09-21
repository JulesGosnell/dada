package org.omo.core;

import java.util.Collection;
import java.util.Map;

public class AbstractMapView<InputKey, InputValue> extends AbstractView<InputKey, InputValue> {

	protected final Map<InputKey, InputValue> map;
	
	public AbstractMapView(Map<InputKey, InputValue> map) {
		this.map = map;
	}
	
	@Override
	protected Object getLock() {
		return map;
	}

	@Override
	public void delete(InputKey key) {
		synchronized (map) {
			map.remove(key);
		}
	}

	@Override
	public void batch(Collection<InputValue> insertions, Collection<InputValue> updates, Collection<InputKey> deletions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(InputValue value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void insert(InputValue value) {
		// TODO Auto-generated method stub
		
	}

}
