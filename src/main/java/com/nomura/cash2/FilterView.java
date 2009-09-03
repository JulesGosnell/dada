/**
 * 
 */
package com.nomura.cash2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FilterView<T> implements ModelView<T> {

	protected final Query<T> query;
	protected final LinkedList<T> results;
	protected final List<View<T>> views = new ArrayList<View<T>>();
	
	public FilterView(Query<T> query) {
		this.query = query;
		this.results = new LinkedList<T>();
	}
	
	@Override
	public void registerView(View<T> view) {
		// call this listener back with full resultset
		view.upsert(results);
		// TODO: collapse remote listeners on same topic into a single ref-counted listener
		views.add(view);
	}
	
	@Override
	public void deregisterView(View<T> view) {
		// TODO: if collapsed, dec ref-count and possible remove ref-counted listener
		// else
		views.remove(view);
	}
	
	// TODO: is there a difference between insertions and updates ?
	// TODO: how can we handle them efficiently in a concurrent environment

	// Listener
	
	@Override
	public void upsert(T upsertion) {
		if (query.apply(upsertion)) {
			results.addFirst(upsertion);
			for (View<T> listener : views)
				listener.upsert(upsertion);
		}
	}

	@Override
	public void upsert(List<T> upsertions) {
		List<T> relevantUpdates = query.apply(upsertions);
		if (!relevantUpdates.isEmpty())
			for (View<T> listener : views)
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
