package org.dada.core;

import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

public class SparseOpenLazyViewTableTestCase extends TestCase {

	public void test() {
		ConcurrentHashMap<Integer, View<Integer, Datum<Integer>>> map = new ConcurrentHashMap<Integer, View<Integer, Datum<Integer>>>();
		
		Factory<Integer, View<Integer, Datum<Integer>>> factory = new Factory<Integer, View<Integer, Datum<Integer>>>() {
			@Override
			public View<Integer, Datum<Integer>> create(Integer key)throws Exception {
				throw new UnsupportedOperationException("NYI");
			}
		};
		new SparseOpenLazyViewTable<Integer, Datum<Integer>>(map, factory);
	}
}
