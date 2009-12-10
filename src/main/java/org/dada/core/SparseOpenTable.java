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

import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If asked for a value that is not present create one using given factory, add it to our map and return
 * it - all in a thread-safe manner.
 *
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public class SparseOpenTable<K, V> implements Table<K, V> {

	private static final Logger LOG = LoggerFactory.getLogger(SparseOpenTable.class);

	public static interface Factory<K, V> {
		public V create(K key) throws Exception;
	}

	private final SparseOpenTable.Factory<K, V> factory;
	private final ConcurrentMap<K, V> map;

	public SparseOpenTable(ConcurrentMap<K, V> map, SparseOpenTable.Factory<K, V> factory) {
		this.factory = factory;
		this.map = map;
	}


	@Override
	public V get(K key) {
		V value = map.get(key);
		if (value == null) {
			// N.B. race condition here - pay attention...
			try {
			        V newValue = factory.create(key); // create potential new entry
				V oldValue = map.putIfAbsent(key, newValue); // insert it IFF still not present
				if (oldValue == null)
				    return newValue; // we won the race - we become new value
				else
				    return oldValue; // we lost the race - we will be thrown away
			} catch (Exception e) {
				LOG.error("unable to create new Table item", e);
			}
		}
		return value;
	}

	@Override
	public V put(K key, V value) {
		return map.put(key, value);
	}

//	@Override
//	public V rem(K key, V value) {
//		boolean removed = map.remove(key, value);
//		return removed ? value : null;
//
//	}
}
