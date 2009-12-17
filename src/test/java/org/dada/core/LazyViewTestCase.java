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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import junit.framework.TestCase;

public class LazyViewTestCase extends TestCase {

	protected int numUpdates;
	
	private Collection<Update<Datum<Integer>>> empty = Collections.emptyList();
	
	private ConcurrentMap<Integer, View<Integer, Datum<Integer>>> map = new ConcurrentHashMap<Integer, View<Integer,Datum<Integer>>>();
	
	private View<Integer, Datum<Integer>> realView = new View<Integer, Datum<Integer>>() {
		@Override
		public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
			numUpdates++;
		}
	};
	
	
	public void testWorkingFactory() throws Exception {
		Integer key = 0;

		Factory<Integer, View<Integer, Datum<Integer>>> factory = new Factory<Integer, View<Integer, Datum<Integer>>>() {
			
			@Override
			public View<Integer, Datum<Integer>> create(Integer key) {
				return realView;
			}
		};
		
		View<Integer, Datum<Integer>> lazyView = new LazyView<Integer, Datum<Integer>>(map, key, factory);
		map.put(key, lazyView); // IMPORTANT - Lazy View REPLACES itself in Map
		
		// force LazyView to initialise - replacing itself in Map
		assertTrue(numUpdates == 0);
		lazyView.update(empty, empty, empty);
		assertTrue(numUpdates == 1);
		assertTrue(map.get(key) == realView);
		
		// check LazyView is still dispatching updates on real View
		lazyView.update(empty, empty, empty);
		assertTrue(numUpdates == 2);

		// what happens if we call init() when the view has already been initialised (coverage)
		((LazyView<Integer, Datum<Integer>>)lazyView).init();
	}
	
	public void testBrokenFactory() {
		Integer key = 0;

		// what happens if the View Factory throws an Exception ?
		Factory<Integer, View<Integer, Datum<Integer>>> factory = new Factory<Integer, View<Integer, Datum<Integer>>>() {
			
			@Override
			public View<Integer, Datum<Integer>> create(Integer key) {
				throw new RuntimeException();
			}
		};
		View<Integer, Datum<Integer>> lazyView = new LazyView<Integer, Datum<Integer>>(map, key, factory);

		assertTrue(numUpdates == 0);
		lazyView.update(empty, empty, empty);
		assertTrue(numUpdates == 0);
	}
}
