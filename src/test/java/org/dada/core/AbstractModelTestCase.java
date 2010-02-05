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
import java.util.List;

import junit.framework.TestCase;

public class AbstractModelTestCase extends TestCase {

	public void testAbstractModel() {

		String name = "MODEL";

		Datum<Integer> datum = new DatumImpl<Integer>(0, 0) {

			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};

		final Collection<Datum<Integer>> data = Collections.singleton(datum);

		Metadata<Integer, Datum<Integer>> metadata = new Metadata<Integer, Datum<Integer>>() {

			@Override
			public List<String> getAttributeNames() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public Object getAttributeValue(Datum<Integer> value, int index) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public Integer getKey(Datum<Integer> value) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public Class<?> getValueClass() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public List<Class<?>> getAttributeTypes() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public List<Getter<?, Datum<Integer>>> getAttributeGetters() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public Collection<String> getKeyAttributeNames() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}
		};

		AbstractModel<Integer, Datum<Integer>> model = new AbstractModel<Integer, Datum<Integer>>(name, metadata) {

			@Override
			public Collection<Datum<Integer>> getData() {
				return data;
			}
		};

		assertTrue(model.getName() == name);
		assertTrue(model.getMetadata() == metadata);

		// deregistration of non-existant view
		model.deregisterView(null);

		final Collection<Update<Datum<Integer>>> empty = Collections.emptyList();

		// well-behaved view
		View<Integer, Datum<Integer>> goodView = new View<Integer, Datum<Integer>>() {

			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
				assertTrue(insertions == empty);
				assertTrue(updates == empty);
				assertTrue(deletions == empty);
			}

		};

		Registration<Integer, Datum<Integer>> registration = model.registerView(goodView);
		assertTrue(registration.getMetadata() == metadata);
		assertTrue(registration.getData().size() == 1);
		assertTrue(registration.getData().contains(datum));

		model.notifyUpdate(empty, empty, empty);

		model.deregisterView(goodView);

		// badly behaved view
		View<Integer, Datum<Integer>> badView = new View<Integer, Datum<Integer>>() {

			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
				throw new UnsupportedOperationException("BADLY BEHAVED VIEW");
			}

		};

		model.registerView(badView);
		model.notifyUpdate(empty, empty, empty);
		model.deregisterView(badView);

		// TODO: demonstrate that badly behaved view does not prevent updates being received by well behaved view...
	}
}
