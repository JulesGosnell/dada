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

// TODO
// I don't like this class - it is too complicated...

public class VersionedModelView<K, V> extends AbstractModelView<K, V> {

	private final Getter<K, V> keyGetter;
	private final Getter<Integer, V> versionGetter;
	
	private final Object mapsLock = new Object(); // only needed by writers ...
	public volatile Maps maps = new Maps(PersistentTreeMap.EMPTY, PersistentTreeMap.EMPTY); // TODO: encapsulate

	// TODO: keyGetter, versionGetter should be retrieved from Metadata
	public VersionedModelView(String name, Metadata<K, V> metadata, Getter<K, V> keyGetter, Getter<Integer, V> versionGetter) {
		super(name, metadata);
		this.keyGetter = keyGetter;
		this.versionGetter = versionGetter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<V> getData() {
		return ((PersistentTreeMap) maps.getCurrent()).values();
	}

	// Model.Lifecycle

	@SuppressWarnings("unchecked")
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> alterations, Collection<Update<V>> deletions) {
		// copy these aggressively for the moment - later they should be copy-on-write or persistent...
		Collection<Update<V>> insertionsOut = new ArrayList<Update<V>>(insertions.size());
		Collection<Update<V>> alterationsOut = new ArrayList<Update<V>>(alterations.size());
		Collection<Update<V>> deletionsOut = new ArrayList<Update<V>>(deletions.size());

		logger.debug("{}: update: insertions={}, alterations={}, deletions={}", name, insertions.size(), alterations.size(), deletions.size());

		// TODO: lose later
		if (insertions.isEmpty() && alterations.isEmpty() && deletions.isEmpty())
			logger.warn("wasteful message: 0 size update");

		synchronized (mapsLock) { // take lock before snapshotting and until replacing maps with new version
			final Maps snapshot = maps;
			final IPersistentMap originalCurrent = snapshot.getCurrent();
			final IPersistentMap originalHistoric = snapshot.getHistoric();
			IPersistentMap current = originalCurrent;
			IPersistentMap historic = originalHistoric;
			boolean changed = false;
			for (Update<V> insertion : insertions) {
				V newValue = insertion.getNewValue();
				K key = keyGetter.get(newValue);
				V currentValue = (V) current.valAt(key);
				if (currentValue == null) {
					current = current.assoc(key, newValue);
					insertionsOut.add(insertion);
					changed = true;
				} else if (versionGetter.get(currentValue) < versionGetter.get(newValue)) {
					current = current.assoc(key, newValue);
					alterationsOut.add(new Update<V>(currentValue, newValue));
					changed = true;
				} else {
					logger.trace("ignoring insertion: {} is more recent than {}", currentValue, newValue);
				}
			}
			for (Update<V> update : alterations) {
				V newValue = update.getNewValue();
				K key = keyGetter.get(newValue);
				V currentValue = (V) current.valAt(key);
				if (currentValue == null || versionGetter.get(currentValue) < versionGetter.get(newValue)) {
					current = current.assoc(keyGetter.get(newValue), newValue);
					alterationsOut.add(update);
					changed = true;
				} else {
					logger.trace("ignoring update: {} is more recent than {}", currentValue, newValue);
				}
			}
			for (Update<V> deletion : deletions) {
				V newValue = deletion.getNewValue();
				K key = keyGetter.get(newValue);
				V currentValue = (V) current.valAt(key);
				if (currentValue != null && versionGetter.get(currentValue) < versionGetter.get(newValue)) {
					try {
						current = current.without(key);
					} catch (Exception e) {
						// TODO: difficult to cover this bit of code - needs some thought...
						logger.warn("unable to perform deletion: {}", e, key);
					}
					historic = historic.assoc(keyGetter.get(newValue), newValue);
					deletionsOut.add(deletion);
					changed = true;
				} else {
					logger.trace("ignoring deletion: {} is more recent than {} or did not exist", currentValue, newValue);
				}
			}

			// have we made any updates - commit them...
			if (changed)
				maps = new Maps(current, historic);
		} // end of sync block

		notifyUpdate(insertionsOut, alterationsOut, deletionsOut);
	}
}

