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
	public void delete(Collection<InputKey> deletions) {
		synchronized (map) {
			for (InputKey deletion : deletions)
			map.remove(deletion);
		}
	}

	@Override
	public void delete(InputKey deletion) {
		synchronized (map) {
			map.remove(deletion);
		}
	}

	@Override
	public void upsert(Collection<InputValue> upsertions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void upsert(InputValue upsertion) {
		// TODO Auto-generated method stub

	}

}
