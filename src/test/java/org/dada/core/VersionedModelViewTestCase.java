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

import junit.framework.TestCase;

public class VersionedModelViewTestCase extends TestCase {

	private VersionedModelView<Integer, Datum<Integer>> view;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Getter<Integer, Datum<Integer>> idGetter = new Getter<Integer, Datum<Integer>>() {@Override public Integer get(Datum<Integer> value) {return value.getId();}};
		Getter<Integer, Datum<Integer>> versionGetter = new Getter<Integer, Datum<Integer>>() {@Override public Integer get(Datum<Integer> value) {return value.getVersion();}};
		view = new VersionedModelView<Integer, Datum<Integer>>(null, null, idGetter, versionGetter);
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

		Datum<Integer> datum0v1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0v1)), nil, nil);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0v1);
		assertTrue(view.maps.historic.count() == 0);

		// out of order insertion - should be update but we need to exercise code...
		
		Datum<Integer> datum0v0 = new IntegerDatum(0, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0v0)), nil, nil);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0v1);
		assertTrue(view.maps.historic.count() == 0);
		
		// correctly ordered insertion - should be update but we need to exercise code...
		
		Datum<Integer> datum0v2 = new IntegerDatum(0, 2);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0v2)), nil, nil);

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0v2);
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

		// out of order deletion of existing val
		
		view.update(nil, nil, Collections.singleton(new Update<Datum<Integer>>(datum1v2, datum1v1)));

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

		// deletion of non-existing val
		
		Datum<Integer> datum2v0 = new IntegerDatum(2, 0);
		Datum<Integer> datum2v1 = new IntegerDatum(2, 1);
		view.update(nil, nil, Collections.singleton(new Update<Datum<Integer>>(datum2v0, datum2v1)));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(!view.maps.current.containsKey(2));
		// TODO: is this  a bug ? do we care ?
		// assertTrue(view.maps.historic.count() == 2);
		// assertTrue(view.maps.historic.valAt(2) == datum2v1);
	}

}
