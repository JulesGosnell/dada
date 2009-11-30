package org.omo.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * Pretend to be a View and, when prodded, create a real one, delegate all extant invocations onto it
 * and replace ourselves in an upstream ConcurrentMap with it...
 * 
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public class LazyView<K, V> implements View<K, V> {
	
	private final ConcurrentMap<Integer, View<K, V>> map;
	private final int key;
	private final ViewFactory<K, V> viewFactory;
	
	private volatile View<K, V> view; // allocated lazily
	
	public LazyView(ConcurrentMap<Integer, View<K, V>> map, int key, ViewFactory<K, V> factory) {
		this.map = map;
		this.key = key;
		this.viewFactory = factory;
	}
	
	private synchronized void init() {
		view = viewFactory.create();
		map.replace(key, this, view);
	}

	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		if (view == null) init();
		view.update(insertions, updates, deletions);
	}

}