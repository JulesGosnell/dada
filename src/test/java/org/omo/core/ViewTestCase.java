package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class ViewTestCase extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		datumMetadata = new IntrospectiveMetadata<Integer, BooleanDatum>(BooleanDatum.class, "Id");
		stringDatumMetadata = new IntrospectiveMetadata<Integer, StringDatum>(StringDatum.class, "Id");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	static class BooleanDatum extends IntegerDatum {
		
		final boolean flag;

		BooleanDatum(int id, boolean flag) {
			super(id, 0);
			this.flag = flag;
		}
		
		public boolean getFlag() {
			return flag;
		}
		
	};

	protected Metadata<Integer, BooleanDatum> datumMetadata;
	protected Metadata<Integer, StringDatum> stringDatumMetadata;
	
	static class DatumIsTrueFilter implements Filter<BooleanDatum> {

		@Override
		public boolean apply(BooleanDatum value) {
			return value.flag;
		}
		
	}
	
	static class Counter<K, V> implements View<K, V> {

		int count;

		@Override
		public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
			count += insertions.size();
		}

		
	};

	public void testSimpleView() {
		ModelView<Integer, BooleanDatum> view = new FilteredModelView<Integer, BooleanDatum>("IsTrue", datumMetadata, new DatumIsTrueFilter());
		Counter<Integer, BooleanDatum> counter = new Counter<Integer, BooleanDatum>();
		Collection<Update<BooleanDatum>> insertions = new ArrayList<Update<BooleanDatum>>(); 
		for (BooleanDatum datum : view.registerView(counter).getData())
			insertions.add(new Update<BooleanDatum>(null, datum));
		counter.update(insertions, new ArrayList<Update<BooleanDatum>>(), new ArrayList<Update<BooleanDatum>>());
		view.update(Collections.singleton(new Update<BooleanDatum>(null, new BooleanDatum(0, false))), new ArrayList<Update<BooleanDatum>>(), new ArrayList<Update<BooleanDatum>>());
		view.update(Collections.singleton(new Update<BooleanDatum>(null, new BooleanDatum(1, true))), new ArrayList<Update<BooleanDatum>>(), new ArrayList<Update<BooleanDatum>>());
		view.update(Collections.singleton(new Update<BooleanDatum>(null, new BooleanDatum(2, false))), new ArrayList<Update<BooleanDatum>>(), new ArrayList<Update<BooleanDatum>>());
		assertTrue(counter.count==1);
	}

	class IsTrueFilter implements Filter<StringDatum> {
		
		public boolean apply(StringDatum value) {
			return value.flag;
		}
	}

	class IsNullFilter implements Filter<StringDatum> {
		
		public boolean apply(StringDatum value) {
			return value.string==null;
		}
	}
	
	public void testCompoundQuery() {
		IsTrueFilter isTrue = new IsTrueFilter();
		IsNullFilter isNull = new IsNullFilter();
		Filter<StringDatum> isTrueAndIsNull = new AndFilter<StringDatum>(isTrue, isNull);
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(0, false, "")));
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(1, true, "")));
		assertTrue(!isTrueAndIsNull.apply(new StringDatum(2, false, null)));
		assertTrue(isTrueAndIsNull.apply(new StringDatum(3, true, null)));
	}

	public void testCompoundView() {
		Counter<Integer, StringDatum> counter = new Counter<Integer, StringDatum>();
		ModelView<Integer, StringDatum> isTrue = new FilteredModelView<Integer, StringDatum>("IsTrue", stringDatumMetadata, new IsTrueFilter());
		ModelView<Integer, StringDatum> isNull = new FilteredModelView<Integer, StringDatum>("IsNull", stringDatumMetadata, new IsNullFilter());
		Collection<Update<StringDatum>> isNullData = new ArrayList<Update<StringDatum>>();  
		for (StringDatum datum : isTrue.registerView(isNull).getData()) {
			isNullData.add(new Update<StringDatum>(null, datum));
		}
		isNull.update(isNullData, new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		Collection<Update<StringDatum>> counterData= new ArrayList<Update<StringDatum>>();  
		for (StringDatum datum : isNull.registerView(counter).getData())
			counterData.add(new Update<StringDatum>(null, datum));
		counter.update(counterData, new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		isTrue.update(Collections.singleton(new Update<StringDatum>(null,new StringDatum(0, false, ""))), new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		assertTrue(counter.count==0);
		isTrue.update(Collections.singleton(new Update<StringDatum>(null,new StringDatum(1, true, ""))), new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		assertTrue(counter.count==0);
		isTrue.update(Collections.singleton(new Update<StringDatum>(null,new StringDatum(2, false, null))), new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		assertTrue(counter.count==0);
		isTrue.update(Collections.singleton(new Update<StringDatum>(null,new StringDatum(3, true, null))), new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		assertTrue(counter.count==1);
		
		List<Update<StringDatum>> insertions = new ArrayList<Update<StringDatum>>(4);
		insertions.add(new Update<StringDatum>(null, new StringDatum(4, false, "")));
		insertions.add(new Update<StringDatum>(null, new StringDatum(5, true, "")));
		insertions.add(new Update<StringDatum>(null, new StringDatum(6, false, null)));
		insertions.add(new Update<StringDatum>(null, new StringDatum(7, true, null)));
		isTrue.update(insertions, new ArrayList<Update<StringDatum>>(), new ArrayList<Update<StringDatum>>());
		assertTrue(counter.count==2);
	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	// TODO: how do we support modification/removal using this architecture ?
	// TODO: modification will require exlusion/versioning
	// TODO: removal will require a deleted flag/status
	
}
