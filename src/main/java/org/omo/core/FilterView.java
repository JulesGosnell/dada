/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class FilterView<Key,Value> extends AbstractModel<Key, Value> implements ModelView<Key, Value, Key, Value> {

	// Lifecycle
	
	public void start() {
	}

	public void stop() {
	}
	
	protected final Query<Value> query;
	protected final LinkedList<Value> results;
	
	public FilterView(String name, Query<Value> query) {
		super(name);
		this.query = query;
		this.results = new LinkedList<Value>();
	}
	
	// Model
	
	protected Collection<Value> getData() {
		return results;
	}
	
	// TODO: is there a difference between insertions and updates ?
	// TODO: how can we handle them efficiently in a concurrent environment

	// Listener
	
	@Override
	public void upsert(Value upsertion) {
		if (query.apply(upsertion)) {
			results.addFirst(upsertion);
			for (View<Key, Value> listener : views)
				listener.upsert(upsertion);
		}
	}

	@Override
	public void upsert(Collection<Value> upsertions) {
		List<Value> relevantUpdates = query.apply(upsertions);
		if (!relevantUpdates.isEmpty())
			for (View<Key, Value> view : views)
				view.upsert(relevantUpdates);
	}

	@Override
	public void delete(Collection<Key> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(int deletion) {
		throw new UnsupportedOperationException("NYI");
	}
}
