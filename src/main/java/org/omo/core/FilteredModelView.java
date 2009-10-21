/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

// TODO: collect together insertions and deliver to aggregator in single collection...

public class FilteredModelView<K, V extends Datum<K>> extends AbstractModelView<K,V> {

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
	
	// TODO: this method is too long and complicated...
	@Override
	public void update(Collection<V> updates) {
		//log.trace(name + ": update: " + updates);
		// TODO: too long/complicated - simplify...
		List<V> updates2 = new ArrayList<V>();
		int potentialSize = updates.size();
		List<V> aggregatorInsertions = new ArrayList<V>(potentialSize);
		synchronized (mapsLock) { // take lock before snapshotting and until replacing maps with new version
			final Maps snapshot = maps;
			final IPersistentMap originalCurrent = snapshot.getCurrent();
			final IPersistentMap originalHistoric = snapshot.getHistoric();
			IPersistentMap current = originalCurrent;
			IPersistentMap historic = originalHistoric;
			for (V newValue : updates) {
				
				// TODO: if version==0, then we needn't check for existing versions - this is the first...
				
				final K key = newValue.getId();
				final V oldCurrentValue = (V)current.valAt(key);
				if (oldCurrentValue != null) {
					if (oldCurrentValue.getVersion() >= newValue.getVersion()) {
						// ignore out of sequence update...
						logger.trace("ignoring: {} is more recent than {}", oldCurrentValue, newValue);
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
								logger.trace("retiring: filter rejection: {}", newValue);
								for (Aggregator<? extends Object, V> aggregator : aggregators)
									aggregator.remove(oldCurrentValue);
							}  catch (Exception e) {
								logger.error("unexpected problem retiring value");
							}
						}
					}
				} else {
					// has it already been retired ?
					final V oldHistoricValue = (V)historic.valAt(key);
					if (oldHistoricValue != null) {
						if (oldHistoricValue.getVersion() >= newValue.getVersion()) {
							// ignore out of sequence update...
							logger.trace("ignoring: " + oldHistoricValue + "is more recent than " + newValue);
						} else {
							if (filter(newValue)) {
								// unretire value
								try {
									current = current.assoc(key, newValue);
									historic = historic.without(key);
									updates2.add(newValue);
									logger.trace("un-retiring: filter acceptance: " + newValue);
									aggregatorInsertions.add(newValue);
								} catch (Exception e) {
									logger.error("unexpected problem unretiring value");
								}
							} else {
								// bring retired version up to date
								historic = historic.assoc(key, newValue);
								updates2.add(newValue);
								// TODO: do aggregators worry about retired values ? 
								//for (Aggregator<? extends Object, V> aggregator : aggregators)
								//	aggregator.update(oldHistoricValue, newValue);
							}
						}
					} else {
						if (filter(newValue)) {
							// adopt new value
							current = current.assoc(key, newValue); 
							updates2.add(newValue);
							aggregatorInsertions.add(newValue);
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
		if (aggregatorInsertions.size() > 0) {
			for (Aggregator<? extends Object, V> aggregator : aggregators)
				aggregator.insert(aggregatorInsertions);
		}
	}
}