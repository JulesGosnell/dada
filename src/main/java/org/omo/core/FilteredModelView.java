/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class FilteredModelView<K, V extends Datum> extends AbstractModelView<K,V> {

	private final Object mapsLock = new Object(); // only needed by writers ...
	public volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY); // TODO: encapsulate
	private final Filter<V> query;
	
	public FilteredModelView(String name, Metadata<K, V> metadata, Filter<V> query) {
		super(name, metadata);
		this.query = query;
	}
	
	public Collection<V> getValues() {
		return ((PersistentTreeMap)maps.getCurrent()).values();
	}
	
	protected boolean filter(V value) {
		return query.apply(value);
	}

	// Model.Lifecycle
	
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
							for (Aggregator<? extends Object, V> aggregator : aggregators)
								aggregator.update(oldCurrentValue, newValue);
						} else {
							// retire value
							try {
								current = current.without(key);
								historic = historic.assoc(key, newValue);
								updates2.add(newValue);
								for (Aggregator<? extends Object, V> aggregator : aggregators)
									aggregator.remove(oldCurrentValue);
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
									for (Aggregator<? extends Object, V> aggregator : aggregators)
										aggregator.insert(newValue);
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
							for (Aggregator<? extends Object, V> aggregator : aggregators)
								aggregator.insert(newValue);
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