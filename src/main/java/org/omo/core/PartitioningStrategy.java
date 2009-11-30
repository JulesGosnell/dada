/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.Collections;

import org.omo.core.Router.Strategy;

public class PartitioningStrategy<K,V extends Datum<Integer>> implements Strategy<K, V> {

	private final Collection<View<K, V>>[] views;
	private final int numViews;
	
	public PartitioningStrategy(Collection<View<K, V>> views) {
		numViews = views.size();
		this.views = new Collection[numViews];
		int i = 0;
		for (View<K, V> partition : views)
			this.views[i++] = Collections.singleton(partition);
	}
	
	@Override
	public boolean getMutable() {
		return false;
	}

	@Override
	public int getRoute(V value) {
		return value.getId() % numViews;
	}

	@Override
	public Collection<View<K, V>> getViews(int route) {
		return views[route];
	}
	
}