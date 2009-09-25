/**
 * 
 */
package org.omo.core.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Datum;
import org.omo.core.Metadata;
import org.omo.core.Model;
import org.omo.core.Query;
import org.omo.core.Registration;
import org.omo.core.Update;
import org.omo.core.View;

import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import clojure.lang.PersistentTreeMap;
import clojure.lang.PersistentTreeSet;

public class ModelView<K, V extends Datum> implements Model<K,V>, View<K, V> {

	private final Log log = LogFactory.getLog(getClass());
	
	private final String name;
	private final Metadata<K, V> metadata;
	
	private final Object viewsLock = new Object();
	private volatile IPersistentSet views = PersistentTreeSet.EMPTY;
	
	private final Object mapsLock = new Object(); // only needed by writers ...
	public volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY); // TODO: encapsulate
	private final Query<V> query;
	
	public ModelView(String name, Metadata<K, V> metadata, Query<V> query) {
		this.name = name;
		this.metadata = metadata;
		this.query = query;
	}
	
	protected boolean filter(V value) {
		return query.apply(value);
	}

	// Model.Lifecycle
	
	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	// Model
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Registration<K, V> registerView(View<K, V> view) {
		synchronized (viewsLock) {
			views = (IPersistentSet)views.cons(view); 
		}
		return new Registration<K, V>(metadata, ((PersistentTreeMap)maps.getCurrent()).values()); // TODO: hack
	}

	@Override
	public boolean deregisterView(View<K, V> view) {
		try {
			synchronized (viewsLock) {
				views = views.disjoin(view);
			}
		} catch (Exception e) {
			log.error("unable to deregister view: " + view);
		}
		return true;
	}
	
	protected void notifyUpdate(Collection<V> values) {
		IPersistentSet snapshot = views;
		for (View<K, V> view : (Iterable<View<K, V>>)snapshot) {
			view.batch(values, new ArrayList<Update<V>>(), new ArrayList<K>());
		}
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
		batch(Collections.singleton(newValue), new ArrayList<Update<V>>(), new ArrayList<K>());
	}

	@Override
	public void update(V oldValue, V newValue) {
		batch(Collections.singleton(newValue), new ArrayList<Update<V>>(), new ArrayList<K>());
	}

	@Override
	public void delete(K key) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void batch(Collection<V> insertions, Collection<Update<V>> notUsed1, Collection<K> notUsed2) {
		// TODO: too long/complicated - simplify...
		synchronized (mapsLock) { // take lock before snapshotting and until replacing maps with new version
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