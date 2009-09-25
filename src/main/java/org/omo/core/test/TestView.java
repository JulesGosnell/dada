/**
 * 
 */
package org.omo.core.test;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Datum;
import org.omo.core.Query;
import org.omo.core.Update;
import org.omo.core.View;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class TestView<K, V extends Datum> implements View<K, V> {

	private final Log log = LogFactory.getLog(getClass());
	private final Object lock = new Object(); // only needed by writers ...
	public volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY); // TODO: encapsulate
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
	public void batch(Collection<V> insertions, Collection<Update<V>> notUsed1, Collection<K> notUsed2) {
		// TODO: too long/complicated - simplify...
		synchronized (lock) { // take lock before snapshotting and until replacing maps with new version
			final Maps snapshot = maps;
			final IPersistentMap originalCurrent = snapshot.getCurrent();
			final IPersistentMap originalHistoric = snapshot.getHistoric();
			IPersistentMap current = originalCurrent;
			IPersistentMap historic = originalHistoric;
			for (V newValue : insertions) {
				final int key = newValue.getId();
				final V oldCurrentValue = (V)current.valAt(key);
				if (oldCurrentValue != null) {
					if (oldCurrentValue.getVersion() >= newValue.getVersion()) {
						// ignore out of sequence update...
					} else {
						if (filter(newValue)) {
							// update current value
							current = current.assoc(key, newValue);
						} else {
							// retire value
							try {
								current = current.without(key);
								historic = historic.assoc(key, newValue);
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
									current = current.assoc(key, newValue);
									historic = historic.without(key);
								} catch (Exception e) {
									log.error("unexpected problem unretiring value");
								}
							} else {
								// bring retired version up to date
								historic = historic.assoc(key, newValue);
							}
						}
					} else {
						if (filter(newValue)) {
							// adopt new value
							current = current.assoc(key, newValue); 
						} else {
							// ignore value
						}
					}
				}
			}

			if (current != originalCurrent || historic!=originalHistoric)
				maps = new Maps(current, historic);
		}
	}
	
}