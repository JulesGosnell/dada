package org.omo.core;

import java.util.concurrent.ConcurrentMap;

/**
 * Given a Map of routes to Views and a View factory, ensure that routes are
 * correctly populated in a timely (i.e. as we first route to them) and thread-safe manner. 
 *
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public class SparseOpenLazyViewTable<K, V> extends SparseOpenTable<View<K, V>> {

	public SparseOpenLazyViewTable(final ConcurrentMap<Integer, View<K, V>> map, final ViewFactory<K, V> factory) {
		super(
				map,
				new SparseOpenTable.Factory<View<K, V>>() {
					public View<K, V> create(Integer key, ConcurrentMap<Integer, View<K, V>> map) {
						return new LazyView<K, V>(map, key, factory);
					}
				}
			);
	}
}
