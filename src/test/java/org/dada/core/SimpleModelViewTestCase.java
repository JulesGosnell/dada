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

import org.dada.core.Datum;
import org.dada.core.IntegerDatum;
import org.dada.core.Registration;
import org.dada.core.SimpleModelView;
import org.dada.core.Update;
import org.dada.core.View;

import junit.framework.TestCase;

public class SimpleModelViewTestCase extends TestCase {

	private SimpleModelView<Integer, Datum<Integer>> view;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		view = new SimpleModelView<Integer, Datum<Integer>>(null, null);
	}

	@Override
	protected void tearDown() throws Exception {
		view = null;
		super.tearDown();
	}

	public void testView() {

		ArrayList<Update<Datum<Integer>>> nil = new ArrayList<Update<Datum<Integer>>>(); // useful value

		// Model/View is empty
		Collection<Datum<Integer>> data = view.getData();

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 0);
		assertTrue(data.isEmpty());
	
		// empty update

		view.update(nil, nil, nil);
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 0);
		assertTrue(data.isEmpty());
	
		// simple insertion
		
		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0)), nil, nil);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update non-existant/contiguous val - where is version 0 ?
		
		IntegerDatum datum1v1 = new IntegerDatum(1, 1);
		view.update(nil, Collections.singleton(new Update<Datum<Integer>>(null, datum1v1)), nil);

		assertTrue(view.maps.current.count() == 2);
		assertTrue(view.maps.current.valAt(1) == datum1v1);
		assertTrue(view.maps.historic.count() == 0);

		// update existing value with contiguous version

		Datum<Integer> datum1v2 = new IntegerDatum(1, 2);
		view.update(nil, Collections.singleton(new Update<Datum<Integer>>(null, datum1v2)), nil);

		assertTrue(view.maps.current.count() == 2);
		assertTrue(view.maps.current.valAt(1) == datum1v2);
		assertTrue(view.maps.historic.count() == 0);
		
		// update existing val with previous version - should be ignored
		
		Datum<Integer> datum1v0 = new IntegerDatum(1, 0);
		view.update(nil, Collections.singleton(new Update<Datum<Integer>>(null, datum1v0)), nil);

		assertTrue(view.maps.current.count() == 2);
		assertTrue(view.maps.current.valAt(1) == datum1v2);
		assertTrue(view.maps.historic.count() == 0);

		// deletion of existing val
		
		Datum<Integer> datum1v3 = new IntegerDatum(1, 3);
		view.update(nil, nil, Collections.singleton(new Update<Datum<Integer>>(datum1v2, datum1v3)));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(!view.maps.current.containsKey(1));
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(1) == datum1v3);
	}

}
