package org.omo.cash2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.omo.cash2.AbstractQuery;
import org.omo.cash2.AndQuery;
import org.omo.cash2.FilterView;
import org.omo.cash2.Query;
import org.omo.cash2.View;

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
	
	static class Counter<T> implements View<T> {

		int count;
		
		@Override
		public void upsert(Collection<T> upsertions) {
			count += upsertions.size();
		}

		@Override
		public void upsert(T upsertion) {
			count++;
		}
		
		@Override
		public void delete(Collection<Integer> deletions) {
			throw new UnsupportedOperationException("NYI");
		}

		@Override
		public void delete(int deletion) {
			throw new UnsupportedOperationException("NYI");
		}

		
	};

	public void testSimpleView() {
		FilterView<Datum> view = new FilterView<Datum>(new DatumIsTrueQuery());
		Counter<Datum> counter = new Counter<Datum>();
		Collection<Datum> data = view.registerView(counter);
		counter.upsert(data);
		view.upsert(new Datum(false));
		view.upsert(new Datum(true));
		view.upsert(new Datum(false));
		assertTrue(counter.count==1);
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
		FilterView<StringDatum> isTrue = new FilterView<StringDatum>(new IsTrueQuery());
		FilterView<StringDatum> isNull = new FilterView<StringDatum>(new IsNullQuery());
		Collection<StringDatum> isNullData = isTrue.registerView(isNull);
		isNull.upsert(isNullData);
		Collection<StringDatum> counterData = isNull.registerView(counter);
		counter.upsert(counterData);
		isTrue.upsert(new StringDatum(false, ""));
		assertTrue(counter.count==0);
		isTrue.upsert(new StringDatum(true, ""));
		assertTrue(counter.count==0);
		isTrue.upsert(new StringDatum(false, null));
		assertTrue(counter.count==0);
		isTrue.upsert(new StringDatum(true, null));
		assertTrue(counter.count==1);
		
		List<StringDatum> elements = new ArrayList<StringDatum>(4);
		elements.add(new StringDatum(false, ""));
		elements.add(new StringDatum(true, ""));
		elements.add(new StringDatum(false, null));
		elements.add(new StringDatum(true, null));
		isTrue.upsert(elements);
		assertTrue(counter.count==2);
	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	// TODO: how do we support modification/removal using this architecture ?
	// TODO: modification will require exlusion/versioning
	// TODO: removal will require a deleted flag/status
	
}
