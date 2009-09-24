package org.omo.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;
import clojure.lang.APersistentMap;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class NewTestCase extends TestCase {

	public static class Maps implements Serializable {

		private final IPersistentMap current;
		private final IPersistentMap historic;
		
		public Maps(IPersistentMap current, IPersistentMap historic) {
			this.current = current;
			this.historic = historic;
		}

		public IPersistentMap getCurrent() {
			return current;
		}

		public IPersistentMap getHistoric() {
			return historic;
		}

	}
	
	class TestView<K, V extends Datum> implements View<K, V> {

		private final Log log = LogFactory.getLog(getClass());
		private volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY);
		private final Query<V> query;
		
		public TestView(Query<V> query) {
			this.query = query;
		}
		
		protected boolean filter(V value) {
			return query.apply(value);
		}

		// View

		// only one thread may write new maps at any one time...
		// how do we test this ?
		// how do we simplify this ?
		// how do we integrate this ?
		// adding notification code may help with testing - 6 cases:
		// - update current
		// - don't update current
		// - retire current
		// - update historic
		// - don't update historic
		// - unretire historic
		// should be easy to collapse two branches into one submethod...
		@Override
		public void insert(V newValue) {
			final Maps snapshot = maps;
			final IPersistentMap current = snapshot.getCurrent();
			final IPersistentMap historic = snapshot.getHistoric();
			final int key = newValue.getId();
			final V oldCurrentValue = (V)current.valAt(key);
			if (oldCurrentValue != null) {
				if (oldCurrentValue.getVersion() >= newValue.getVersion()) {
					// ignore out of sequence update...
				} else {
					if (filter(newValue)) {
						// update current value
						maps = new Maps(current.assoc(key, newValue), historic);
					} else {
						// retire value
						try {
							maps = new Maps(current.without(key), historic.assoc(key, newValue));
						}  catch (Exception e) {
							log.error("unexpected problem retiring value");
						}
					}
				}
			} else {
				// has it already been retired ?
				final V oldHistoricValue = (V)historic.valAt(key);
				if (oldHistoricValue != null) {
					if (oldHistoricValue.getVersion() >= newValue.getVersion()) {
						// ignore out of sequence update...
					} else {
						if (filter(newValue)) {
							// unretire value
							try {
								IPersistentMap newHistoric = historic.without(key);
								maps = new Maps(current.assoc(key, newValue), newHistoric);
							} catch (Exception e) {
								log.error("unexpected problem unretiring value");
							}
						} else {
							// bring retired version up to date
							maps = new Maps(current, historic.assoc(key, newValue));
						}
					}
				} else {
					if (filter(newValue)) {
						// adopt this value
						maps = new Maps(current.assoc(key, newValue), historic); 
					} else {
						// ignore value
					}
				}
			}
		}

		@Override
		public void update(V oldValue, V newValue) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("NYI");
		}

		@Override
		public void delete(K key) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("NYI");
		}

		@Override
		public void batch(Collection<V> insertions, Collection<Update<V>> updates, Collection<K> deletions) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("NYI");
		}
		

	};
	
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
	private TestView<Integer, Datum> view;
	
	protected void setUp() throws Exception {
		super.setUp();
		query = new TestQuery<Datum>();
		view = new TestView<Integer, Datum>(query);
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
		view.insert(datum0);
		
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update existing current value
		Datum datum1 = new TestDatum(0, 1);
		view.insert(datum1);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum1);
		assertTrue(view.maps.historic.count() == 0);
		
		// out of sequence updates
		Datum datum2 = new TestDatum(0, 2);
		Datum datum3 = new TestDatum(0, 3);
		view.insert(datum3);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		view.insert(datum2);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		query.setAnswer(false);
		
		// rejected value
		Datum reject = new TestDatum(1, 0);
		view.insert(reject);
		 
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		// retire existing value
		Datum datum4 = new TestDatum(0, 4);
		view.insert(datum4);
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum4);
		
		// update a retired value
		Datum datum5 = new TestDatum(0, 5);
		view.insert(datum5);

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum5);

		// update a retired value out of sequence
		Datum datum6 = new TestDatum(0, 6);
		Datum datum7 = new TestDatum(0, 7);
		view.insert(datum7);
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);

		view.insert(datum6);

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);
		
		// unretire retired value
		query.setAnswer(true);
		Datum datum8 = new TestDatum(0, 8);
		view.insert(datum8);
		
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum8);
		assertTrue(view.maps.historic.count() == 0);
	}
	
	
}
