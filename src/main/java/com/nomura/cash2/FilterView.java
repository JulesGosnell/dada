/**
 * 
 */
package com.nomura.cash2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class FilterView<T> implements View<T> {

	final Query<T> query;
	final LinkedList<T> results;
	final List<Listener<T>> listeners = new ArrayList<Listener<T>>();
	
	public FilterView(Query<T> query) {
		this.query = query;
		this.results = new LinkedList<T>();
	}
	
	@Override
	public void addElementListener(Listener<T> listener) {
		// call this listener back with full resultset
		listener.update(results);
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
	public void update(T update) {
		if (query.apply(update)) {
			results.addFirst(update);
			for (Listener<T> listener : listeners)
				listener.update(update);
		}
		
	}

	@Override
	public void update(List<T> updates) {
		List<T> relevantUpdates = query.apply(updates);
		if (!relevantUpdates.isEmpty())
			for (Listener<T> listener : listeners)
				listener.update(relevantUpdates);
	}
}