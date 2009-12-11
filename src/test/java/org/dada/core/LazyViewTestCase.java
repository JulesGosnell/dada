package org.dada.core;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import junit.framework.TestCase;

public class LazyViewTestCase extends TestCase {

	protected boolean updated;
	
	public void test() {
		ConcurrentMap<Integer, View<Integer, Datum<Integer>>> map = new ConcurrentHashMap<Integer, View<Integer,Datum<Integer>>>();
		
		Integer key = 0;
		
		final View<Integer, Datum<Integer>> item = new View<Integer, Datum<Integer>>() {
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
				updated = true;
			}
		};
		
		ViewFactory<Integer, Datum<Integer>> factory = new ViewFactory<Integer, Datum<Integer>>() {
			
			@Override
			public View<Integer, Datum<Integer>> create(Integer key) {
				return item;
			}
		};
		
		View<Integer, Datum<Integer>> view = new LazyView<Integer, Datum<Integer>>(map, key, factory);
		map.put(key, view); // IMPORTANT - Lazy View REPLACES itself in Map
		Collection<Update<Datum<Integer>>> empty = Collections.emptyList();
		view.update(empty, empty, empty);
		
		assertTrue(map.get(key) == item);
		
	}
}
