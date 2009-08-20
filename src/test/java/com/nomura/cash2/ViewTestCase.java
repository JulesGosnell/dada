package com.nomura.cash2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class ViewTestCase extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	static class Datum {
		
		final boolean flag;

		Datum(boolean flag) {
			this.flag = flag;
		}
		
	};

	static interface Query<T> {
		boolean apply(T element);
		LinkedList<T> apply(List<T> elements);
	}
	
	static abstract class AbstractQuery<T> implements Query<T> {
		
		@Override
		public LinkedList<T> apply(List<T> elements) {
			LinkedList<T> results = new LinkedList<T>();
			for (T element : elements)
				if (apply(element))
					results.addFirst(element);
			return results;
		}
	}
	
	static class DatumIsTrueQuery extends AbstractQuery<Datum> {

		@Override
		public boolean apply(Datum element) {
			return element.flag;
		}
		
	}
	
	public void testSimpleQuery() {

		LinkedList<Datum> data = new LinkedList<Datum>();
		Datum d1=new Datum(false);
		data.addFirst(d1);
		Datum d2=new Datum(true);
		data.add(d2);
		List<Datum> results = new DatumIsTrueQuery().apply(data);
		assertTrue(results.size()==1);
		assertTrue(results.get(0)==d2);
	}
	
	static interface Listener<T> {
		
		void update(List<T> updates);
		void update(T update);
		
	};
	
	static class View<T> implements Listener<T> {
	
		final Query<T> query;
		final LinkedList<T> results;
		final List<Listener<T>> listeners = new ArrayList<Listener<T>>();
		
		public View(Query<T> query) {
			this.query = query;
			this.results = new LinkedList<T>();
		}
		
		public void addElementListener(Listener<T> listener) {
			// call this listener back with full resultset
			listener.update(results);
			// TODO: collapse remote listeners on same topic into a single ref-counted listener
			listeners.add(listener);
		}
		
		public void removeElementListener(Listener<T> listener) {
			// TODO: if collapsed, dec ref-count and possible remove ref-counted listener
			// else
			listeners.remove(listener);
		}
		
		// TODO: is there a difference between insertions and updates ?
		// TODO: how can we handle them efficiently in a concurrent environment

		// Listener
		
		public void update(T update) {
			if (query.apply(update)) {
				results.addFirst(update);
				for (Listener<T> listener : listeners)
					listener.update(update);
			}
			
		}
		
		public void update(List<T> updates) {
			List<T> relevantUpdates = query.apply(updates);
			if (!relevantUpdates.isEmpty())
				for (Listener<T> listener : listeners)
					listener.update(relevantUpdates);
		}
	}
	
	static class Counter<T> implements Listener<T> {

		int count;
		
		@Override
		public void update(List<T> updates) {
			count += updates.size();
		}

		@Override
		public void update(T update) {
			count++;
		}
		
	};

	public void testSimpleView() {
		View<Datum> view = new View<Datum>(new DatumIsTrueQuery());
		Counter<Datum> counter = new Counter<Datum>();
		view.addElementListener(counter);
		view.update(new Datum(false));
		view.update(new Datum(true));
		view.update(new Datum(false));
		assertTrue(counter.count==1);
	}

	static class AndQuery<T> implements Query<T> {
		
		Query<T> lhs;
		Query<T> rhs;
		
		AndQuery(Query<T> lhs, Query<T> rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		@Override
		public boolean apply(T element) {
			return lhs.apply(element) && rhs.apply(element);
		}

		@Override
		public LinkedList<T> apply(List<T> elements) {
			return rhs.apply(lhs.apply(elements));
		}

	}

	class StringDatum extends Datum {
		String string;
		
		StringDatum(boolean flag, String string) {
			super(flag);
			this.string = string;
		}
	}
	
	class IsTrueQuery extends AbstractQuery<StringDatum> {
		
		public boolean apply(StringDatum element) {
			return element.flag;
		}
	}

	class IsNullQuery extends AbstractQuery<StringDatum> {
		
		public boolean apply(StringDatum element) {
			return element.string==null;
		}
	}
	
	public void testCompoundQuery() {
		IsTrueQuery isTrue = new IsTrueQuery();
		IsNullQuery isNull = new IsNullQuery();
		Query<StringDatum> isTrueAndIsNull = new AndQuery<StringDatum>(isTrue, isNull);
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(false, "")));
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(true, "")));
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(false, null)));
		assertTrue(isTrueAndIsNull.apply(new StringDatum(true, null)));
	}

	public void testCompoundView() {
		Counter<StringDatum> counter = new Counter<StringDatum>();
		View<StringDatum> isTrue = new View<StringDatum>(new IsTrueQuery());
		View<StringDatum> isNull = new View<StringDatum>(new IsNullQuery());
		isTrue.addElementListener(isNull);
		isNull.addElementListener(counter);
		isTrue.update(new StringDatum(false, ""));
		assertTrue(counter.count==0);
		isTrue.update(new StringDatum(true, ""));
		assertTrue(counter.count==0);
		isTrue.update(new StringDatum(false, null));
		assertTrue(counter.count==0);
		isTrue.update(new StringDatum(true, null));
		assertTrue(counter.count==1);
		
		List<StringDatum> elements = new ArrayList<StringDatum>(4);
		elements.add(new StringDatum(false, ""));
		elements.add(new StringDatum(true, ""));
		elements.add(new StringDatum(false, null));
		elements.add(new StringDatum(true, null));
		isTrue.update(elements);
		assertTrue(counter.count==2);
	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	
}
