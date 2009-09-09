package org.omo.core;

import java.util.Collection;
import java.util.Map;

public class AbstractMapView<Input> extends AbstractView<Input> {

	protected final Map<Integer, Input> map;
	
	public AbstractMapView(Map map) {
		this.map = map;
	}
	
	@Override
	protected Object getLock() {
		return map;
	}

	@Override
	public void delete(Collection<Integer> deletions) {
		synchronized (map) {
			for (Integer deletion : deletions)
			map.remove(deletion);
		}
	}

	@Override
	public void delete(int deletion) {
		synchronized (map) {
			map.remove(deletion);
		}
	}

	@Override
	public void upsert(Collection<Input> upsertions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void upsert(Input upsertion) {
		// TODO Auto-generated method stub

	}

}
