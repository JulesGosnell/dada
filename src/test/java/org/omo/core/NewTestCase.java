package org.omo.core;

import java.util.Collection;

import junit.framework.TestCase;
import clojure.lang.APersistentMap;
import clojure.lang.PersistentTreeMap;

public class NewTestCase extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	interface FilterConfig {
		
		// yes
		//  is it already a current item
		//  yes
		//   does the newer item have a higher version number than the current ?
		//   yes
		//    replace the current version with the new one
		//    fire an UPDATE
		//   no
		//    ignore the new item
		//    [fire an IGNORE]
		//  no
		//   is it a historic item ?
	}
	
	class TestView<K, V extends Datum> implements View<K, V> {

		protected final Object writeLock = new Object();
		protected volatile APersistentMap current = PersistentTreeMap.EMPTY;
		protected volatile APersistentMap historic = PersistentTreeMap.EMPTY;
		
		protected boolean filter(V value) {
			return true;
		}
		

		@Override
		public void insert(V newValue) {
			// does the item satisfy filter criteria ?
			APersistentMap newCurrent = null;
			APersistentMap newHistoric = null;
			if (filter(newValue)) {
				if ((newCurrent = insert(newValue, current)) == null &&  (newHistoric = insert(newValue, historic)) == null) {
					
				} else {
					//   is it a historic item ?
					V oldHistoricValue = (V)historic.get(newValue.getId());

				}
			} else {
				
			}
		}
		
		/**
		 * If this returns null, the newvalue has been completely ignored.
		 * If the output Map is the same as the input Map, the newValue was already present in a newer version and so has been ignored.
		 * If the output Map is different from the input Map, the newvalue has been accepted
		 * @param newValue
		 * @param map
		 * @return
		 */
		protected APersistentMap insert(V newValue, APersistentMap map) {
			V oldCurrentValue = (V)map.get(newValue.getId());
			//  is it already present
			if (oldCurrentValue != null) {
				//  does the new value have a higher version number than the old ?
				if (newValue.getVersion() > oldCurrentValue.getVersion()) {
					// replace the current version with the new one
					return (APersistentMap)map.assoc(newValue.getId(), newValue);
					// TODO: fire an UPDATE
				} else {
					// ignore newValue - it must have arrived out of sequence...
					return map;
					// TODO: [fire an IGNORE]
					// TODO: LOG SOMETHING HERE...
				}
			} else {
				return null;
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
		
		// View

	};
	
	public void testModel() {

	}
	
	
}
