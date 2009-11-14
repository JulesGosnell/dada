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
	
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		// copy these aggressively for the moment - later they should be copy-on-write or persistent...
		Collection<Update<V>> insertionsOut = new ArrayList<Update<V>>(insertions.size());
		Collection<Update<V>> updatesOut = new ArrayList<Update<V>>(updates.size());
		Collection<Update<V>> deletionsOut = new ArrayList<Update<V>>(deletions.size());
		if (logger.isDebugEnabled())
			logger.debug("{}: update: insertions={}, updates={}, deletions={}", new Object[]{name, insertions.size(), updates.size(), deletions.size()});

		// TODO: lose later
		if (insertions.isEmpty() && updates.isEmpty() && deletions.isEmpty())
			logger.warn("wasteful message: 0 size update", new RuntimeException());

		synchronized (mapsLock) { // take lock before snapshotting and until replacing maps with new version
			final Maps snapshot = maps;
			final IPersistentMap originalCurrent = snapshot.getCurrent();
			final IPersistentMap originalHistoric = snapshot.getHistoric();
			IPersistentMap current = originalCurrent;
			IPersistentMap historic = originalHistoric;
			for (Update<V> insertion : insertions) {
				V newValue = insertion.getNewValue();
				K key = newValue.getId();
				V currentValue = (V)current.valAt(key);
				if (currentValue == null || currentValue.getVersion() < newValue.getVersion()) {
					current = current.assoc(key, newValue);
					insertionsOut.add(insertion);
				} else {
					logger.trace("ignoring insertion: {} is more recent than {}", currentValue, newValue);
				}
			}
			for (Update<V> update : updates) {
				V newValue = update.getNewValue();
				K key = newValue.getId();
				V currentValue = (V)current.valAt(key);
				if (newValue.getVersion() > currentValue.getVersion()) {
					current = current.assoc(newValue.getId(), newValue);
					updatesOut.add(update);
				} else {
					logger.trace("ignoring update: {} is more recent than {}", currentValue, newValue);
				}
			}
			for (Update<V> deletion : deletions) {
				V newValue = deletion.getNewValue();
				K key = newValue.getId();
				V currentValue = (V)current.valAt(key);
				if (currentValue != null && currentValue.getVersion() < newValue.getVersion()) {
					try {
						current = current.without(key);
					} catch (Exception e) {
						logger.warn("unable to perform deletion: {}", key, e);
					}
					historic = historic.assoc(newValue.getId(), newValue);
					deletionsOut.add(deletion);
				} else {
					logger.trace("ignoring deletion: {} is more recent than {} or did not exist", currentValue, newValue);
				}
			}

			// have we made any updates - commit them...
			if (current != originalCurrent || historic!=originalHistoric)
				maps = new Maps(current, historic);
		} // end of sync block

		// notify Viewers/Aggregators
		// TODO: merge Aggregator and View
		if (insertionsOut.size() >0 || updatesOut.size() > 0 || deletionsOut.size() > 0) {
			// temp adaptor
			Collection<V> viewUpdates = new ArrayList<V>();
			for (Update<V> insertion : insertionsOut) {
				viewUpdates.add(insertion.getNewValue());
			}
			for (Update<V> update : updatesOut) {
				viewUpdates.add(update.getNewValue());
				for (Aggregator<? extends Object, V> aggregator : aggregators)
					aggregator.update(update.getOldValue(), update.getNewValue());
			}
			// deletions ?
			for (Update<V> deletion : deletionsOut) {
				// Views ?
				for (Aggregator<? extends Object, V> aggregator : aggregators)
					aggregator.remove(deletion.getOldValue());
			}
			// TODO: View api needs fixing
			notifyUpdate(viewUpdates);
		}
	}
	
	// TODO: this method is too long and complicated...
	public void oldUpdate(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		logger.debug("{}: update: size={}", name, insertions.size());
		
		// TODO: lose later
		if (insertions.size() < 1)
			logger.warn("wasteful message: 0 size update", new RuntimeException());
		
		// TODO: too long/complicated - simplify...
		List<V> updates2 = new ArrayList<V>();
		int potentialSize = insertions.size();
		List<V> aggregatorInsertions = new ArrayList<V>(potentialSize);
		synchronized (mapsLock) { // take lock before snapshotting and until replacing maps with new version
			final Maps snapshot = maps;
			final IPersistentMap originalCurrent = snapshot.getCurrent();
			final IPersistentMap originalHistoric = snapshot.getHistoric();
			IPersistentMap current = originalCurrent;
			IPersistentMap historic = originalHistoric;
			for (Update<V> insertion : insertions) {
				V newValue = insertion.getNewValue();
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
						        logger.trace("ignoring: {} is more recent than {}", oldHistoricValue, newValue);
						} else {
							if (filter(newValue)) {
								// unretire value
								try {
									current = current.assoc(key, newValue);
									historic = historic.without(key);
									updates2.add(newValue);
									logger.trace("un-retiring: filter acceptance: {}", newValue);
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
