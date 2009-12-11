package org.dada.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.dada.core.Router.Strategy;

import junit.framework.TestCase;

public class RouterTestCase extends TestCase {

	public void testMutable() {
		test(true);
	}
	
	public void testImmutable() {
		test(false);
	}
	
	public void test(final boolean mutable) {
		View<Integer, Datum<Integer>> view = new View<Integer, Datum<Integer>>() {
			
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions,
					Collection<Update<Datum<Integer>>> updates,
					Collection<Update<Datum<Integer>>> deletions) {
			}
		};
		
		final Collection<View<Integer, Datum<Integer>>> views =Collections.singleton(view);
		
		Strategy<Integer, Datum<Integer>> strategy = new Strategy<Integer, Datum<Integer>>() {

			@Override
			public boolean getMutable() {
				return mutable;
			}

			@Override
			public int getRoute(Datum<Integer> value) {
				return 0;
			}

			@Override
			public Collection<View<Integer, Datum<Integer>>> getViews(int route) {
				return views;
			}
		};

		View<Integer, Datum<Integer>> router = new Router<Integer, Datum<Integer>>(strategy);

		Collection<Update<Datum<Integer>>> nil = Collections.emptyList();

		DatumImpl<Integer> v0 = new DatumImpl<Integer>(0, 0) {
			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};
		Update<Datum<Integer>> insertion = new Update<Datum<Integer>>(null, v0);
		Collection<Update<Datum<Integer>>> insertions = Collections.singleton(insertion);
		router.update(insertions, nil, nil);

		DatumImpl<Integer> v1 = new DatumImpl<Integer>(0, 1) {
			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};
		Update<Datum<Integer>> update = new Update<Datum<Integer>>(v0, v1);
		Collection<Update<Datum<Integer>>> updates = Collections.singleton(update);
		router.update(nil, updates, nil);

		DatumImpl<Integer> v2 = new DatumImpl<Integer>(0, 2) {
			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};
		Update<Datum<Integer>> deletion = new Update<Datum<Integer>>(v1, v2);
		Collection<Update<Datum<Integer>>> deletions = Collections.singleton(deletion);
		router.update(nil, nil, deletions);
	}
}
