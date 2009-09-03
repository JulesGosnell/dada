/**
 * 
 */
package com.nomura.cash2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FilterView<T> implements View<T> {

	protected final Query<T> query;
	protected final LinkedList<T> results;
	protected final List<Listener<T>> listeners = new ArrayList<Listener<T>>();
	
	public FilterView(Query<T> query) {
		this.query = query;
		this.results = new LinkedList<T>();
	}
	
	@Override
	public void addElementListener(Listener<T> listener) {
		// call this listener back with full resultset
		listener.upsert(results);
		// TODO: collapse remote listeners on same topic into a single ref-counted listener
		listeners.add(listener);
	}
	
	@Override
	public void removeElementListener(Listener<T> listener) {
		// TODO: if collapsed, dec ref-count and possible remove ref-counted listener
		// else
		listeners.remove(listener);
	}
	
	// TODO: is there a difference between insertions and updates ?
	// TODO: how can we handle them efficiently in a concurrent environment

	// Listener
	
	@Override
	public void upsert(T upsertion) {
		if (query.apply(upsertion)) {
			results.addFirst(upsertion);
			for (Listener<T> listener : listeners)
				listener.upsert(upsertion);
		}
	}

	@Override
	public void upsert(List<T> upsertions) {
		List<T> relevantUpdates = query.apply(upsertions);
		if (!relevantUpdates.isEmpty())
			for (Listener<T> listener : listeners)
				listener.upsert(relevantUpdates);
	}

	@Override
	public void delete(List<Integer> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(int deletion) {
		throw new UnsupportedOperationException("NYI");
	}
}
