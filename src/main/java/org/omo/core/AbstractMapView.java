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

	// View
	
	@Override
	public void insert(InputValue value) {
		throw new UnsupportedOperationException("NYI");
	}
	
	@Override
	public void update(InputValue oldValue, InputValue newValue) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(InputKey key) {
		synchronized (map) {
			map.remove(key);
		}
	}

	@Override
	public void batch(Collection<InputValue> insertions, Collection<InputValue> updates, Collection<InputKey> deletions) {
		throw new UnsupportedOperationException("NYI");
	}


}
