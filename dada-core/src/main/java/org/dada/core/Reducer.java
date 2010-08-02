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
 * Data2, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.core;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

// TODO: should be some sort of TransformedModelView - since output is a different shape from input

public class Reducer<KI, VI, V, KO, VO> extends AbstractModel<KO, VO> implements View<VI> {

	public interface Strategy<VI, V, KO, VO> {
		V initialValue();
		Class<?> initialType(Class<?> type);
		VO currentValue(Collection<KO> keys, int version, V value);
		V reduce(Collection<Update<VI>> insertions, Collection<Update<VI>> alterations, Collection<Update<VI>> deletions);
		V apply(V currentValue, V delta);
	}

	private final Strategy<VI, V, KO, VO> strategy;

	private final Collection<Update<VO>> nil = Collections.emptyList();
	
	private final Collection<KO> keys;
	
	private static class Data2<V> {
		private final int version;
		private final V value;
		
		private Data2(V value, int version) {
			this.value = value;
			this.version = version;
		}
	}

	private final AtomicReference<Data2<V>> Data2;
	
	public Reducer(String name, Metadata<KO, VO> metadata, Collection<KO> keys, Strategy<VI, V, KO, VO> strategy) {
		super(name, metadata);
		this.keys = keys;
		this.strategy = strategy;
		Data2= new AtomicReference<Data2<V>>(new Data2<V>(this.strategy.initialValue(), 0));
	}

	@Override
	public Data<VO> getData() {
		Data2<V> snapshot = Data2.get();
		return new Data<VO>(Collections.singleton(strategy.currentValue(keys, snapshot.version, snapshot.value)), null);
	}

	@Override
	public void update(Collection<Update<VI>> insertions, Collection<Update<VI>> alterations, Collection<Update<VI>> deletions) {
		V delta = strategy.reduce(insertions, alterations, deletions);
		Data2<V> oldData2;
		Data2<V> newData2;
		V oldValue;
		int oldVersion;
		V newValue;
		int newVersion;
		do {
			oldData2= Data2.get();
			oldValue = oldData2.value;
			oldVersion = oldData2.version;
			newValue = strategy.apply(oldValue, delta);
			newVersion = oldVersion + 1;
			newData2= new Data2<V>(newValue, newVersion);
		} while (!Data2.compareAndSet(oldData2, newData2));
		
		notifyUpdate(nil, Collections.singleton(new Update<VO>(strategy.currentValue(keys, oldVersion, oldValue), strategy.currentValue(keys, newVersion, newValue))), nil);
	}

}
