package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.omo.core.AbstractQuery;
import org.omo.core.AndQuery;
import org.omo.core.FilterView;
import org.omo.core.Query;
import org.omo.core.View;

import junit.framework.TestCase;

public class ViewTestCase extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		datumMetadata = new IntrospectiveMetadata<Integer, Datum>(Datum.class, "Id");
		stringDatumMetadata = new IntrospectiveMetadata<Integer, StringDatum>(StringDatum.class, "Id");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	static class Datum {
		
		final int id;
		final boolean flag;

		Datum(int id, boolean flag) {
			this.id = id;
			this.flag = flag;
		}
		
		public int getId() {
			return id;
		}
		
		public boolean getFlag() {
			return flag;
		}
		
	};

	protected Metadata<Integer, Datum> datumMetadata;
	protected Metadata<Integer, StringDatum> stringDatumMetadata;
	
	static class DatumIsTrueQuery extends AbstractQuery<Datum> {

		@Override
		public boolean apply(Datum element) {
			return element.flag;
		}
		
	}
	
	public void testSimpleQuery() {

		LinkedList<Datum> data = new LinkedList<Datum>();
		Datum d1=new Datum(0, false);
		data.addFirst(d1);
		Datum d2=new Datum(1, true);
		data.add(d2);
		List<Datum> results = new DatumIsTrueQuery().apply(data);
		assertTrue(results.size()==1);
		assertTrue(results.get(0)==d2);
	}
	
	static class Counter<K, V> implements View<K, V> {

		int count;
		
		@Override
		public void insert(V value) {
			count++;
		}

		@Override
		public void update(V value) {
			throw new UnsupportedOperationException("NYI");
		}
		
		@Override
		public void delete(K key) {
			throw new UnsupportedOperationException("NYI");
		}

		@Override
		public void batch(Collection<V> insertions, Collection<V> updates, Collection<K> deletions) {
			count += (insertions == null ? 0 : insertions.size());
			if ((updates != null && updates.size()>0) || (deletions!=null && deletions.size()>0))
				throw new UnsupportedOperationException("NYI");
		}

		
	};

	public void testSimpleView() {
		FilterView<Integer, Datum> view = new FilterView<Integer, Datum>("IsTrue", datumMetadata, new DatumIsTrueQuery());
		Counter<Integer, Datum> counter = new Counter<Integer, Datum>();
		Collection<Datum> insertions = view.registerView(counter).getData();
		counter.batch(insertions, null, null);
		view.insert(new Datum(0, false));
		view.insert(new Datum(1, true));
		view.insert(new Datum(2, false));
		assertTrue(counter.count==1);
	}

	class StringDatum extends Datum {
		String string;
		
		StringDatum(int id, boolean flag, String string) {
			super(id, flag);
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
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(0, false, "")));
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(1, true, "")));
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(2, false, null)));
		assertTrue(isTrueAndIsNull.apply(new StringDatum(3, true, null)));
	}

	public void testCompoundView() {
		Counter<Integer, StringDatum> counter = new Counter<Integer, StringDatum>();
		FilterView<Integer, StringDatum> isTrue = new FilterView<Integer, StringDatum>("IsTrue", stringDatumMetadata, new IsTrueQuery());
		FilterView<Integer, StringDatum> isNull = new FilterView<Integer, StringDatum>("IsNull", stringDatumMetadata, new IsNullQuery());
		Collection<StringDatum> isNullData = isTrue.registerView(isNull).getData();
		isNull.batch(isNullData, null, null);
		Collection<StringDatum> counterData = isNull.registerView(counter).getData();
		counter.batch(counterData, null, null);
		isTrue.insert(new StringDatum(0, false, ""));
		assertTrue(counter.count==0);
		isTrue.insert(new StringDatum(1, true, ""));
		assertTrue(counter.count==0);
		isTrue.insert(new StringDatum(2, false, null));
		assertTrue(counter.count==0);
		isTrue.insert(new StringDatum(3, true, null));
		assertTrue(counter.count==1);
		
		List<StringDatum> insertions = new ArrayList<StringDatum>(4);
		insertions.add(new StringDatum(4, false, ""));
		insertions.add(new StringDatum(5, true, ""));
		insertions.add(new StringDatum(6, false, null));
		insertions.add(new StringDatum(7, true, null));
		isTrue.batch(insertions, null, null);
		assertTrue(counter.count==2);
	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	// TODO: how do we support modification/removal using this architecture ?
	// TODO: modification will require exlusion/versioning
	// TODO: removal will require a deleted flag/status
	
}
