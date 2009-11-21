/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

// TODO: collect together insertions and deliver to aggregator in single collection...

public class SimpleModelView<K, V extends Datum<K>> extends AbstractModelView<K,V> {

	private final Object mapsLock = new Object(); // only needed by writers ...
	public volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY); // TODO: encapsulate
	
	public SimpleModelView(String name, Metadata<K, V> metadata) {
		super(name, metadata);
	}
	
	public Collection<V> getValues() {
		return ((PersistentTreeMap)maps.getCurrent()).values();
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
				if (currentValue == null || currentValue.getVersion() < newValue.getVersion()) {
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

		notifyUpdate(insertionsOut, updatesOut, deletionsOut);
	}
}
	
