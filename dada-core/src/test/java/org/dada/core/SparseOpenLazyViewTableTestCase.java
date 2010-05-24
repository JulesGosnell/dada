package org.dada.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

public class SparseOpenLazyViewTableTestCase extends TestCase {

	public void test() {
		
		final View<Datum<Integer>> view = new View<Datum<Integer>>() {
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> alterations, Collection<Update<Datum<Integer>>> deletions) {
			}
		};
		
		ConcurrentHashMap<Integer, View<Datum<Integer>>> map = new ConcurrentHashMap<Integer, View<Datum<Integer>>>();
		
		Factory<Integer, View<Datum<Integer>>> factory = new Factory<Integer, View<Datum<Integer>>>() {
			@Override
			public View<Datum<Integer>> create(Integer key) throws Exception {
				return view;
			}
		};
		SparseOpenLazyViewTable<Integer, Datum<Integer>> table = new SparseOpenLazyViewTable<Integer, Datum<Integer>>(map, factory);
		
		
		View<Datum<Integer>> lazyView = table.get(0);
		lazyView.update(null, null, null); // force lazy to activate and replace itself in table...
		
		assertTrue(table.get(0) == view);
	}
}
