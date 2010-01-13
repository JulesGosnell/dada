package org.dada.core;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

public class PartitioningStrategyTestCase extends TestCase {

	public class TestView implements View<Integer, Datum<Integer>> {
		@Override
		public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
			throw new UnsupportedOperationException("NYI");
		}
	};
	
	public void test() {
		Collection<View<Integer, Datum<Integer>>> views = new ArrayList<View<Integer,Datum<Integer>>>();
		TestView view0 = new TestView();
		views.add(view0);
		TestView view1 = new TestView(); 
		views.add(view1);
		Getter<Integer, Datum<Integer>> getter = new Getter<Integer, Datum<Integer>>() {
			@Override
			public Integer get(Datum<Integer> value) {
				return value.getId();
			}
		};
		PartitioningStrategy<Integer, Datum<Integer>> strategy = new PartitioningStrategy<Integer, Datum<Integer>>(getter, views);
		
		assertFalse(strategy.getMutable());
		assertTrue(strategy.getRoute(new IntegerDatum(0, 0)) == 0);
		assertTrue(strategy.getRoute(new IntegerDatum(1, 0)) == 1);
		assertTrue(strategy.getRoute(new IntegerDatum(2, 0)) == 0);
		assertTrue(strategy.getRoute(new IntegerDatum(3, 0)) == 1);
		
		Collection<View<Integer, Datum<Integer>>> views0 = strategy.getViews(0);
		Collection<View<Integer, Datum<Integer>>> views1 = strategy.getViews(1);
		assertTrue(views0.size() == 1);
		assertTrue(views0.iterator().next() == view0);
		assertTrue(views1.size() == 1);
		assertTrue(views1.iterator().next() == view1);
	}
}
