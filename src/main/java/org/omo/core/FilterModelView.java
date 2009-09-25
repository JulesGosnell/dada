/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class FilterModelView<K,V> extends AbstractModel<K, V> implements ModelView<K, V, K, V> {

	// Lifecycle
	
	public void start() {
	}

	public void stop() {
	}
	
	protected final Query<V> query;
	protected final LinkedList<V> results;
	
	public FilterModelView(String name, Metadata<K, V> metadata, Query<V> query) {
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
	public void update(Collection<V> updates) {
		List<V> relevantInsertions = query.apply(updates);
		if (!relevantInsertions.isEmpty())
			for (View<K, V> view : views)
				view.update(relevantInsertions);
	}

}
