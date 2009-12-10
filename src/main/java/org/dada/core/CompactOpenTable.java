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

import java.util.List;

/**
 * If asked for a value that is not present create one using given factory, add it to our map and return
 * it - all in a thread-safe manner.
 *
 * @author jules
 *
 * @param <V>
 */
public class CompactOpenTable<V> implements Table<Integer, V> {

	private final Factory<Integer, V> factory;
	private final List<V> values;

	public CompactOpenTable(List<V> values, Factory<Integer, V> factory) {
		this.factory = factory;
		this.values = values;
	}


	@Override
	public V get(Integer key) {
		V value = values.get(key);
		if (value == null) {
			// pay careful attention here - plenty of scope for error...
			throw new UnsupportedOperationException("NYI");
		}
		return value;
	}

	@Override
	public V put(Integer key, V value) {
		return values.set(key, value);
	}

//	@Override
//	public V rem(Integer key, V value) {
//		values.remove(key);
//		throw new UnsupportedOperationException("NYI");
//		// TODO: check...
//		//return value;
//	}
}
