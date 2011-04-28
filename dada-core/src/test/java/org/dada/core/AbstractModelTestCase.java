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

import org.jmock.integration.junit3.MockObjectTestCase;

public class AbstractModelTestCase extends MockObjectTestCase {

	public void testAbstractModel() {

		String name = "MODEL";

		Datum<Integer> datum = new DatumImpl<Integer>(0, 0) {

			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};

		final Collection<Datum<Integer>> data = Collections.singleton(datum);

		Metadata<Integer, Datum<Integer>> metadata = mock(Metadata.class);
		AbstractModel<Integer, Datum<Integer>> model = new AbstractModel<Integer, Datum<Integer>>(name, metadata) {

			@Override
			public Data<Datum<Integer>> getData() {
				return new Data<Datum<Integer>>(data, null);
			}

			@Override
			public Datum<Integer> find(Integer key) {
				// TODO Auto-generated method stub
				// return null;
				throw new UnsupportedOperationException("NYI");
			}
		};

		assertTrue(model.getName() == name);
		assertTrue(model.getMetadata() == metadata);

		// deregistration of non-existant view
		model.detach(null);

		final Collection<Update<Datum<Integer>>> empty = Collections.emptyList();

		// well-behaved view
		View<Datum<Integer>> goodView = new View<Datum<Integer>>() {

			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> alterations, Collection<Update<Datum<Integer>>> deletions) {
				assertTrue(insertions == empty);
				assertTrue(alterations == empty);
				assertTrue(deletions == empty);
			}

		};

		Data<Datum<Integer>> data2 = model.attach(goodView);
		assertTrue(data2.getExtant().size() == 1);
		assertTrue(data2.getExtant().contains(datum));

		model.notifyUpdate(empty, empty, empty);

		model.detach(goodView);

		// badly behaved view
		View<Datum<Integer>> badView = new View<Datum<Integer>>() {

			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> alterations, Collection<Update<Datum<Integer>>> deletions) {
				throw new UnsupportedOperationException("BADLY BEHAVED VIEW");
			}

		};

		model.attach(badView);
		model.notifyUpdate(empty, empty, empty);
		model.detach(badView);

		// TODO: demonstrate that badly behaved view does not prevent updates being received by well behaved view...
	}
}
