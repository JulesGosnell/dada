package org.omo.core;

import java.util.Collection;

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
	
	static class Counter<K, V> implements View<K, V> {

		int count;

		@Override
		public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
			count += insertions.size();
		}

		
	};
	
	public void testEmpty() {
		
	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	// TODO: how do we support modification/removal using this architecture ?
	// TODO: modification will require exlusion/versioning
	// TODO: removal will require a deleted flag/status
	
}
