/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class FilteredModelView<K, V extends Datum> implements Model<K,V>, View<K, V> {

	private final Log log = LogFactory.getLog(getClass());
	
	private final String name;
	private final Metadata<K, V> metadata;
	
	private final Object viewsLock = new Object();
	private volatile List<View<K, V>> views = new ArrayList<View<K,V>>();
	//private volatile IPersistentSet views = PersistentTreeSet.EMPTY;
	
	private final Object mapsLock = new Object(); // only needed by writers ...
	public volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY); // TODO: encapsulate
	private final Filter<V> query;
	
	public FilteredModelView(String name, Metadata<K, V> metadata, Filter<V> query) {
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
			//views = (IPersistentSet)views.cons(view);
			List<View<K, V>> newViews = new ArrayList<View<K,V>>(views);
			newViews.add(view);
			views = newViews;
		}
		Collection<V> values = ((PersistentTreeMap)maps.getCurrent()).values();
		return new Registration<K, V>(metadata, new ArrayList<V>(values)); // TODO: hack - clojure containers not serialisable
	}

	@Override
	public boolean deregisterView(View<K, V> view) {
		try {
			synchronized (viewsLock) {
				//views = views.disjoin(view);
				List<View<K, V>> newViews = new ArrayList<View<K,V>>(views);
				newViews.remove(view);
				views = newViews;
			}
		} catch (Exception e) {
			log.error("unable to deregister view: " + view);
		}
		return true;
	}
	
	protected void notifyUpdate(Collection<V> values) {
		//IPersistentSet snapshot = views;
		List<View<K, V>> snapshot = views;
		//for (View<K, V> view : (Iterable<View<K, V>>)snapshot) {
		log.info("NfOTIFYING UPDATE ("+getName()+"): "+values);
		for (View<K, V> view : snapshot) {
			view.update(values);
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
	public void update(Collection<V> updates) {
		// TODO: too long/complicated - simplify...
		List<V> updates2 = new ArrayList<V>();
		synchronized (mapsLock) { // take lock before snapshotting and until replacing maps with new version
			final Maps snapshot = maps;
			final IPersistentMap originalCurrent = snapshot.getCurrent();
			final IPersistentMap originalHistoric = snapshot.getHistoric();
			IPersistentMap current = originalCurrent;
			IPersistentMap historic = originalHistoric;
			for (V newValue : updates) {
				final int key = newValue.getId();
				final V oldCurrentValue = (V)current.valAt(key);
				if (oldCurrentValue != null) {
					if (oldCurrentValue.getVersion() >= newValue.getVersion()) {
						// ignore out of sequence update...
					} else {
						if (filter(newValue)) {
							// update current value
							current = current.assoc(key, newValue);
							updates2.add(newValue);
						} else {
							// retire value
							try {
								current = current.without(key);
								historic = historic.assoc(key, newValue);
								updates2.add(newValue);
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
									updates2.add(newValue);
								} catch (Exception e) {
									log.error("unexpected problem unretiring value");
								}
							} else {
								// bring retired version up to date
								historic = historic.assoc(key, newValue);
								updates2.add(newValue);
							}
						}
					} else {
						if (filter(newValue)) {
							// adopt new value
							current = current.assoc(key, newValue); 
							updates2.add(newValue);
						} else {
							// ignore value
						}
					}
				}
			}

			if (current != originalCurrent || historic!=originalHistoric)
				maps = new Maps(current, historic);
		}
		if (updates2.size() > 0)
			notifyUpdate(updates2);
	}

}