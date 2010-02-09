package org.dada.core;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import org.dada.core.Router.Strategy;

public class RouterTestCase extends TestCase {

	public void testRouteOnImmutableAttribute() {
		View<Datum<Integer>> view = new View<Datum<Integer>>() {
			
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions,
					Collection<Update<Datum<Integer>>> updates,
					Collection<Update<Datum<Integer>>> deletions) {
			}
		};
		
		final Collection<View<Datum<Integer>>> views =Collections.singleton(view);
		
		Strategy<Integer, Datum<Integer>> idStrategy = new Strategy<Integer, Datum<Integer>>() {

			@Override
			public boolean getMutable() {
				return false;
			}

			@Override
			public int getRoute(Datum<Integer> value) {
				return value.getId();
			}

			@Override
			public Collection<View<Datum<Integer>>> getViews(int route) {
				return views;
			}
		};

		View<Datum<Integer>> router = new Router<Integer, Datum<Integer>>(idStrategy);

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

	public void testRouteOnmutableAttribute() {
		View<Datum<Integer>> view = new View<Datum<Integer>>() {
			
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions,
					Collection<Update<Datum<Integer>>> updates,
					Collection<Update<Datum<Integer>>> deletions) {
			}
		};
		
		final Collection<View<Datum<Integer>>> views =Collections.singleton(view);
		
		Strategy<Integer, Datum<Integer>> idStrategy = new Strategy<Integer, Datum<Integer>>() {

			@Override
			public boolean getMutable() {
				return true;
			}

			@Override
			public int getRoute(Datum<Integer> value) {
				return value.getVersion();
			}

			@Override
			public Collection<View<Datum<Integer>>> getViews(int route) {
				return views;
			}
		};

		View<Datum<Integer>> router = new Router<Integer, Datum<Integer>>(idStrategy);

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
		
		{
		DatumImpl<Integer> v11 = new DatumImpl<Integer>(1, 1) {
			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};
		Update<Datum<Integer>> update1 = new Update<Datum<Integer>>(v0, v11);
		Collection<Update<Datum<Integer>>> updates1 = Collections.singleton(update1);
		router.update(nil, updates1, nil);
		}
	}
}
