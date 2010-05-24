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
import java.util.List;

import junit.framework.TestCase;



public class RouterAbstractTestCase extends TestCase {

	public class IntegerDatum extends DatumImpl<Integer> {

		protected final int integer;

		public IntegerDatum(int id, int version, int integer) {
			super(id, version);
			this.integer = integer;
		}

		@Override
		public int compareTo(Datum<Integer> that) {
			return this.integer - ((IntegerDatum)that).integer; // TODO: is this right ?
		}

		public Integer getInteger() {
			return integer;
		}

	}

	public static class SignRoutingStrategy implements Router.Strategy<IntegerDatum> {

		protected final Collection<View<IntegerDatum>>[] views;

		@SuppressWarnings("unchecked")
		public SignRoutingStrategy(View<IntegerDatum> negative, View<IntegerDatum> positive) {
			this.views = new Collection[]{Collections.singleton(negative), Collections.singleton(positive), Collections.emptyList()};
		}

		@Override
		public boolean getMutable() {
			return true;
		}

		@Override
		public int getRoute(IntegerDatum value) {
			Integer integer = value.getInteger();
			return integer < 0 ? 0 : integer > 0 ? 1 : 2;
		}

		@Override
		public Collection<View<IntegerDatum>> getViews(int route) {
			return views[route];
		}
	}

	public void testRouter() {

		Getter<Integer, IntegerDatum> idGetter = new Getter<Integer, IntegerDatum>() {
			@Override
			public Integer get(IntegerDatum value) {
				return value.getId();
			}
		};
		Getter<Integer, IntegerDatum> versionGetter = new Getter<Integer, IntegerDatum>() {
			@Override
			public Integer get(IntegerDatum value) {
				return value.getVersion();
			}
		};
		VersionedModelView<Integer, IntegerDatum> negative = new VersionedModelView<Integer, IntegerDatum>("Negative", null, idGetter, versionGetter);
		VersionedModelView<Integer, IntegerDatum> positive = new VersionedModelView<Integer, IntegerDatum>("Positive", null, idGetter, versionGetter);

		View<IntegerDatum> router = new Router<IntegerDatum>(new SignRoutingStrategy(negative, positive));

		IntegerDatum d0v0 = new IntegerDatum(0, 0, -4);
		IntegerDatum d1v0 = new IntegerDatum(1, 0, -3);
		IntegerDatum d2v0 = new IntegerDatum(2, 0, -2);
		IntegerDatum d3v0 = new IntegerDatum(3, 0, -1);
		IntegerDatum d4v0 = new IntegerDatum(4, 0,  0);
		IntegerDatum d5v0 = new IntegerDatum(5, 0,  0);
		IntegerDatum d6v0 = new IntegerDatum(6, 0,  1);
		IntegerDatum d7v0 = new IntegerDatum(7, 0,  2);
		IntegerDatum d8v0 = new IntegerDatum(8, 0,  3);
		IntegerDatum d9v0 = new IntegerDatum(9, 0,  4);

		List<Update<IntegerDatum>> insertions = new ArrayList<Update<IntegerDatum>>(10);
		insertions.add(new Update<IntegerDatum>(null, d0v0));
		insertions.add(new Update<IntegerDatum>(null, d1v0));
		insertions.add(new Update<IntegerDatum>(null, d2v0));
		insertions.add(new Update<IntegerDatum>(null, d3v0));
		insertions.add(new Update<IntegerDatum>(null, d4v0));
		insertions.add(new Update<IntegerDatum>(null, d5v0));
		insertions.add(new Update<IntegerDatum>(null, d6v0));
		insertions.add(new Update<IntegerDatum>(null, d7v0));
		insertions.add(new Update<IntegerDatum>(null, d8v0));
		insertions.add(new Update<IntegerDatum>(null, d9v0));

		List<IntegerDatum> negativeData = new ArrayList<IntegerDatum>();
		negativeData.add(d0v0);
		negativeData.add(d1v0);
		negativeData.add(d2v0);
		negativeData.add(d3v0);
		List<IntegerDatum> positiveData = new ArrayList<IntegerDatum>();
		positiveData.add(d6v0);
		positiveData.add(d7v0);
		positiveData.add(d8v0);
		positiveData.add(d9v0);

		router.update(insertions, new ArrayList<Update<IntegerDatum>>(), new ArrayList<Update<IntegerDatum>>());

		assertTrue(negative.getData().containsAll(negativeData));
		assertTrue(negative.getData().size() == negativeData.size());
		assertTrue(positive.getData().containsAll(positiveData));
		assertTrue(positive.getData().size() == positiveData.size());

		// 'amend' into...
		IntegerDatum d4v1 = new IntegerDatum(d4v0.getId(), d4v0.getVersion()+1, d4v0.getInteger()-1);
		IntegerDatum d5v1 = new IntegerDatum(d5v0.getId(), d5v0.getVersion()+1, d5v0.getInteger()+1);
		List<Update<IntegerDatum>> updates = new ArrayList<Update<IntegerDatum>>(2);
		updates.add(new Update<IntegerDatum>(d4v0, d4v1));
		updates.add(new Update<IntegerDatum>(d5v0, d5v1));

		router.update(new ArrayList<Update<IntegerDatum>>(), updates, new ArrayList<Update<IntegerDatum>>());

		assertTrue(negative.getData().containsAll(negativeData));
		assertTrue(negative.getData().contains(d4v1));
		assertTrue(negative.getData().size() == negativeData.size()+1);
		assertTrue(positive.getData().containsAll(positiveData));
		assertTrue(positive.getData().contains(d5v1));
		assertTrue(positive.getData().size() == positiveData.size()+1);

		// 'amend' away - to another partition
		IntegerDatum d4v2 = new IntegerDatum(d4v1.getId(), d4v1.getVersion()+1, d4v1.getInteger()*-1);
		IntegerDatum d5v2 = new IntegerDatum(d5v1.getId(), d5v1.getVersion()+1, d5v1.getInteger()*-1);
		List<Update<IntegerDatum>> updates2 = new ArrayList<Update<IntegerDatum>>(2);
		updates2.add(new Update<IntegerDatum>(d4v1, d4v2));
		updates2.add(new Update<IntegerDatum>(d5v1, d5v2));

		router.update(new ArrayList<Update<IntegerDatum>>(), updates2, new ArrayList<Update<IntegerDatum>>());

		assertTrue(negative.getData().containsAll(negativeData));
		assertTrue(negative.getData().contains(d5v2));
		assertTrue(negative.getData().size() == negativeData.size()+1);
		assertTrue(positive.getData().containsAll(positiveData));
		assertTrue(positive.getData().contains(d4v2));
		assertTrue(positive.getData().size() == positiveData.size()+1);

		// 'amend' away - to nowhere
		IntegerDatum d4v3 = new IntegerDatum(d4v2.getId(), d4v2.getVersion()+1, 0);
		IntegerDatum d5v3 = new IntegerDatum(d5v2.getId(), d5v2.getVersion()+1, 0);
		List<Update<IntegerDatum>> updates3 = new ArrayList<Update<IntegerDatum>>(2);
		updates3.add(new Update<IntegerDatum>(d4v2, d4v3));
		updates3.add(new Update<IntegerDatum>(d5v2, d5v3));

		router.update(new ArrayList<Update<IntegerDatum>>(), updates3, new ArrayList<Update<IntegerDatum>>());

		assertTrue(!negative.getData().contains(d4v3));
		assertTrue(!negative.getData().contains(d5v3));
		assertTrue(negative.getData().containsAll(negativeData));
		assertTrue(negative.getData().size() == negativeData.size());
		assertTrue(!positive.getData().contains(d4v3));
		assertTrue(!positive.getData().contains(d5v3));
		assertTrue(positive.getData().containsAll(positiveData));
		assertTrue(positive.getData().size() == positiveData.size());

	}
}
