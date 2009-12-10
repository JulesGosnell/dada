/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.core;

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

	@Override
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
				}			}
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

