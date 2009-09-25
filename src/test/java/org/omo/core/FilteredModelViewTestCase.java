package org.omo.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import junit.framework.TestCase;


import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentSet;
import clojure.lang.PersistentTreeSet;

public class FilteredModelViewTestCase extends TestCase {

	class TestQuery<V> implements Query<V> {

		private boolean answer;
		
		public boolean isAnswer() {
			return answer;
		}

		public void setAnswer(boolean answer) {
			this.answer = answer;
		}

		@Override
		public boolean apply(V element) {
			return answer;
		}

		@Override
		public LinkedList<V> apply(Collection<V> elements) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("NYI");
		}
		
	}
	
	private TestQuery<Datum> query;
	private FilteredModelView<Integer, Datum> view;
	
	protected void setUp() throws Exception {
		super.setUp();
		query = new TestQuery<Datum>();
		view = new FilteredModelView<Integer, Datum>(null, null, query);
	}

	protected void tearDown() throws Exception {
		view = null;
		query = null;
		super.tearDown();
	}

	class TestDatum implements Datum {

		private int id;
		private int version;
		
		public TestDatum(int id, int version) {
			this.id = id;
			this.version = version;
		}
		
		@Override
		public int getId() {
			return id;
		}

		@Override
		public int getVersion() {
			return version;
		}

		@Override
		public int compareTo(Datum that) {
			return this.id - that.getId();
		}
		
	}
	
	public void testView() {

		query.setAnswer(true);
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 0);

		// simple insertion
		Datum datum0 = new TestDatum(0, 0);
		view.update(Collections.singleton(datum0));
		
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update existing current value
		Datum datum1 = new TestDatum(0, 1);
		view.update(Collections.singleton(datum1));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum1);
		assertTrue(view.maps.historic.count() == 0);
		
		// out of sequence updates
		Datum datum2 = new TestDatum(0, 2);
		Datum datum3 = new TestDatum(0, 3);
		view.update(Collections.singleton(datum3));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		view.update(Collections.singleton(datum2));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		query.setAnswer(false);
		
		// rejected value
		Datum reject = new TestDatum(1, 0);
		view.update(Collections.singleton(reject));
		 
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		// retire existing value
		Datum datum4 = new TestDatum(0, 4);
		view.update(Collections.singleton(datum4));
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum4);
		
		// update a retired value
		Datum datum5 = new TestDatum(0, 5);
		view.update(Collections.singleton(datum5));

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum5);

		// update a retired value out of sequence
		Datum datum6 = new TestDatum(0, 6);
		Datum datum7 = new TestDatum(0, 7);
		view.update(Collections.singleton(datum7));
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);

		view.update(Collections.singleton(datum6));

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);
		
		// unretire retired value
		query.setAnswer(true);
		Datum datum8 = new TestDatum(0, 8);
		view.update(Collections.singleton(datum8));
		
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum8);
		assertTrue(view.maps.historic.count() == 0);
	}
	

	public void testFoo() throws Exception {
		
		IPersistentSet set = PersistentTreeSet.EMPTY;
		assertTrue(set.count()==0);
		set = (IPersistentSet)set.cons(1);
		assertTrue(set.count()==1);
		set = set.disjoin(1);
		assertTrue(set.count()==0);
	}
	
}

