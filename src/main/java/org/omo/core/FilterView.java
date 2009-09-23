/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class FilterView<K,V> extends AbstractModel<K, V> implements ModelView<K, V, K, V> {

	// Lifecycle
	
	public void start() {
	}

	public void stop() {
	}
	
	protected final Query<V> query;
	protected final LinkedList<V> results;
	
	public FilterView(String name, Metadata<K, V> metadata, Query<V> query) {
		super(name, metadata);
		this.query = query;
		this.results = new LinkedList<V>();
	}
	
	// Model
	
	protected Collection<V> getData() {
		return results;
	}
	
	// TODO: is there a difference between insertions and updates ?
	// TODO: how can we handle them efficiently in a concurrent environment

	// Listener
	
	@Override
	public void insert(V value) {
		if (query.apply(value)) {
			results.addFirst(value);
			for (View<K, V> listener : views)
				listener.insert(value);
		}
	}

	@Override
	public void update(V oldValue, V newValue) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(K key) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void batch(Collection<V> insertions, Collection<Update<V>> updates, Collection<K> deletions) {
		List<V> relevantInsertions = query.apply(insertions);
		if (!relevantInsertions.isEmpty())
			for (View<K, V> view : views)
				view.batch(relevantInsertions, updates, deletions);
		if (updates.size()>0 || deletions.size()>0)
			throw new UnsupportedOperationException("NYI");
		// TODO: extend
	}

}
