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

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 0);

		// simple insertion
		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update existing current value
		Datum<Integer> datum1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum1)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum1);
		assertTrue(view.maps.historic.count() == 0);

		// out of sequence updates
		Datum<Integer> datum2 = new IntegerDatum(0, 2);
		Datum<Integer> datum3 = new IntegerDatum(0, 3);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum3)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum2)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		Datum<Integer> datum8 = new IntegerDatum(0, 8);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum8)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum8);
		assertTrue(view.maps.historic.count() == 0);
	}

	class IdAggregator implements View<Integer, Datum<Integer>> {

		int total = 0;

		public Integer getTotal() {
			return total;
		}

		@Override
		public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
			for (Update<Datum<Integer>> insertion : insertions)
				total += insertion.getNewValue().getId();
			for (Update<Datum<Integer>> update : updates)
				total += update.getNewValue().getId() - update.getOldValue().getId();
			for (Update<Datum<Integer>> deletion : deletions)
				total -= deletion.getOldValue().getId();
		}
	}

	public void DONOTtestAggregator() {
		IdAggregator aggregator = new IdAggregator();
		view.registerView(aggregator);

		assertTrue(view.maps.historic.count() == 0);
		assertTrue(aggregator.getTotal() == 0);

		// simple insertion
		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		ArrayList<Update<Datum<Integer>>> empty = new ArrayList<Update<Datum<Integer>>>();
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0)), empty, empty);
		assertTrue(view.maps.historic.count() == 0);
		assertTrue(aggregator.getTotal() == 0);

		// update existing current value
		Datum<Integer> datum1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum1)), empty, empty);
		assertTrue(aggregator.getTotal() == 0);

		// out of sequence updates
		Datum<Integer> datum2 = new IntegerDatum(0, 2);
		Datum<Integer> datum3 = new IntegerDatum(0, 3);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum3)), empty, empty);
		assertTrue(aggregator.getTotal() == 0);

		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum2)), empty, empty);
		assertTrue(aggregator.getTotal() == 0);

		Datum<Integer> datum8 = new IntegerDatum(0, 8);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum8)), empty, empty);
		assertTrue(aggregator.getTotal() == 0);

		// insert 2nd value
		Datum<Integer> datum9 = new IntegerDatum(1, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum9)), empty, empty);
		assertTrue(aggregator.getTotal() == 1);

		// insert 3rd value
		Datum<Integer> datum10 = new IntegerDatum(2, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum10)), empty, empty);
		assertTrue(aggregator.getTotal() == 3);

		Datum<Integer> datum12 = new IntegerDatum(2, 2);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum12)), empty, empty);
		assertTrue(aggregator.getTotal() == 3);

		// register an aggregator
		IdAggregator aggregator2 = new IdAggregator();
		Registration<Integer, Datum<Integer>> registration = view.registerView(aggregator2);
		Collection<Update<Datum<Integer>>> insertions = empty;
		for (Datum<Integer> datum : registration.getData())
			insertions.add(new Update<Datum<Integer>>(null, datum));
		aggregator2.update(insertions, empty, empty);
		assertTrue(aggregator2.getTotal() == 3);

		Datum<Integer> datum13 = new IntegerDatum(3, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum13)), empty, empty);
		assertTrue(aggregator.getTotal() == 6);
	}
}
