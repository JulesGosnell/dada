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
	
	public FilterView(String name, Metadata<Key, Value> metadata, Query<Value> query) {
		super(name, metadata);
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
	public void insert(Value value) {
		if (query.apply(value)) {
			results.addFirst(value);
			for (View<Key, Value> listener : views)
				listener.insert(value);
		}
	}

	@Override
	public void update(Value value) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(Key key) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void batch(Collection<Value> insertions, Collection<Value> updates, Collection<Key> deletions) {
		List<Value> relevantInsertions = query.apply(insertions);
		if (!relevantInsertions.isEmpty())
			for (View<Key, Value> view : views)
				view.batch(relevantInsertions, null, null);
		if ((updates != null && updates.size()>0) || (deletions != null && deletions.size()>0))
			throw new UnsupportedOperationException("NYI");
	}

}
