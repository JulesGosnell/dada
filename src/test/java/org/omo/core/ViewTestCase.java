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
		public void update(Collection<V> updates) {
			count += updates.size();
		}

		
	};

	public void testSimpleView() {
		ModelView<Integer, BooleanDatum> view = new FilteredModelView<Integer, BooleanDatum>("IsTrue", datumMetadata, new DatumIsTrueFilter());
		Counter<Integer, BooleanDatum> counter = new Counter<Integer, BooleanDatum>();
		Collection<BooleanDatum> insertions = view.registerView(counter).getData();
		counter.update(insertions);
		view.update(Collections.singleton(new BooleanDatum(0, false)));
		view.update(Collections.singleton(new BooleanDatum(1, true)));
		view.update(Collections.singleton(new BooleanDatum(2, false)));
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
		Collection<StringDatum> isNullData = isTrue.registerView(isNull).getData();
		isNull.update(isNullData);
		Collection<StringDatum> counterData = isNull.registerView(counter).getData();
		counter.update(counterData);
		isTrue.update(Collections.singleton(new StringDatum(0, false, "")));
		assertTrue(counter.count==0);
		isTrue.update(Collections.singleton(new StringDatum(1, true, "")));
		assertTrue(counter.count==0);
		isTrue.update(Collections.singleton(new StringDatum(2, false, null)));
		assertTrue(counter.count==0);
		isTrue.update(Collections.singleton(new StringDatum(3, true, null)));
		assertTrue(counter.count==1);
		
		List<StringDatum> insertions = new ArrayList<StringDatum>(4);
		insertions.add(new StringDatum(4, false, ""));
		insertions.add(new StringDatum(5, true, ""));
		insertions.add(new StringDatum(6, false, null));
		insertions.add(new StringDatum(7, true, null));
		isTrue.update(insertions);
		assertTrue(counter.count==2);
	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	// TODO: how do we support modification/removal using this architecture ?
	// TODO: modification will require exlusion/versioning
	// TODO: removal will require a deleted flag/status
	
}
