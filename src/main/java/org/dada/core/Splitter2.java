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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Splitter2<K, GK, V> implements View<V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public interface Factory<K, V> { Collection<View<V>> create(K key); };

	private final boolean mutable;
	private final Getter<K, V> getter;
	private final Map<K, Collection<View<V>>> keyToViews;
	private final Factory<K, V> factory;
	
	public Splitter2(boolean mutable, Getter<K, V> getter, Map<K, Collection<View<V>>> valueToViews, Factory<K, V> factory) {
		this.mutable = mutable;
		this.getter = getter;
		this.keyToViews = valueToViews;
		this.factory = factory;
	}

	private class Batch<V> {
		Collection<Update<V>> insertions;
		Collection<Update<V>>  updates;
		Collection<Update<V>> deletions;
	}

	private Batch<V> get(Map<K, Batch<V>> map, K key) {
		// naive impl to start
		Batch<V> collection = map.get(key);
		if (collection == null)
			map.put(key, collection = new Batch<V>());
		return collection;
	}

	private Collection<Update<V>> NIL = Collections.emptyList();
	
	private Collection<Update<V>> dft(Collection<Update<V>> updates) {
		return updates == null ? NIL : updates;
	}
	
	@Override
	public void update(Collection<Update<V>> insertions,Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		Map<K, Batch<V>> map = new HashMap<K, Batch<V>>();

		// TODO: mutability

		for (Update<V> insertion : insertions) {
			V value = insertion.getNewValue();
			Batch<V> batch = get(map, getter.get(value));
			Collection<Update<V>> i = batch.insertions;
			if (i == null)
				i = (batch.insertions = new ArrayList<Update<V>>());
			i.add(insertion);
		}

		// TODO: updates

		for (Update<V> update : updates) {
			V newValue = update.getNewValue();
			K newAttribute = getter.get(newValue);
			Batch<V> newBatch = get(map, newAttribute);
			K oldAttribute = null; // TODO: outwit compiler ? - is this safe ?
			// TODO: NPEs...
			if (mutable && 
					((oldAttribute = getter.get(update.getOldValue())) != newAttribute) || // they could both be null
					(oldAttribute != null && oldAttribute.equals(newAttribute))) {
				Batch<V> oldBatch = get(map, oldAttribute);
				Collection<Update<V>> d = oldBatch.deletions;
				if (d == null)
					d = (oldBatch.deletions= new ArrayList<Update<V>>());
				d.add(update);
				Collection<Update<V>> i = newBatch.insertions;
				if (i == null)
					i = (newBatch.insertions = new ArrayList<Update<V>>());
				i.add(update);

			} else {
				Collection<Update<V>> u = newBatch.updates;
				if (u == null)
					u = (newBatch.updates = new ArrayList<Update<V>>());
				u.add(update);
			}
		}

		for (Update<V> deletion : deletions) {
			V value = deletion.getOldValue();
			Batch<V> event = get(map, getter.get(value));
			Collection<Update<V>> i = event.deletions;
			if (i == null)
				i = (event.deletions = new ArrayList<Update<V>>());
			i.add(deletion);
		}


		for (Entry<K, Batch<V>> entry : map.entrySet()) {
			K key = entry.getKey();
			Batch<V> event = entry.getValue();
			Collection<View<V>> views = keyToViews.get(key);
			if (views == null) {
				if (factory != null) {
					views = factory.create(key);
				} else {
					views = Collections.emptyList();
				}
			}
			for (View<V> view : views) {
				try {
					view.update(dft(event.insertions), dft(event.updates), dft(event.deletions));
				} catch (Throwable t) {
					logger.error("problem updating view", t);
				}
			}
		}
	}
}
