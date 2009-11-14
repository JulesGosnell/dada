package org.omo.core;

import java.util.ArrayList;
import java.util.List;


import junit.framework.TestCase;

public class PartitioningTestCase extends TestCase {

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
	
	public class SignPartitionMap implements PartitionMap<Integer, IntegerDatum> {
		
		protected final View<Integer, IntegerDatum>[] views;
		
		public SignPartitionMap(View<Integer, IntegerDatum> negative, View<Integer, IntegerDatum> positive) {
			this.views = new View[]{negative, positive};
		}

		@Override
		public boolean containsKey(Integer key) {
			return key >=0 && key < views.length;
		}

		@Override
		public Integer getKey(IntegerDatum value) {
			int integer = value.getInteger();
			if (integer < 0)
				return 0;
			else if (integer > 0)
				return 1;
			else
				return 2;
		}

		@Override
		public View<Integer, IntegerDatum> getPartition(Integer key) {
			if (key < views.length)
				return views[key];
			else
				return null;
		}

		@Override
		public int size() {
			return views.length;
		}
	}
	
	public void testInsertionPartitioning() {

		FilteredModelView<Integer, IntegerDatum> negative = new FilteredModelView<Integer, IntegerDatum>("Negative", null, new IdentityFilter<IntegerDatum>());
		FilteredModelView<Integer, IntegerDatum> positive = new FilteredModelView<Integer, IntegerDatum>("Positive", null, new IdentityFilter<IntegerDatum>());

		SignPartitionMap map = new SignPartitionMap(negative, positive);  
		Partitioner2<Integer, IntegerDatum> partitioner = new Partitioner2<Integer, IntegerDatum>(map);
		
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

		partitioner.update(insertions, new ArrayList<Update<IntegerDatum>>(), new ArrayList<Update<IntegerDatum>>());
		
		assertTrue(negative.getValues().containsAll(negativeData));
		assertTrue(negative.getValues().size() == negativeData.size());
		assertTrue(positive.getValues().containsAll(positiveData));
		assertTrue(positive.getValues().size() == positiveData.size());
		
		// 'amend' into...
		IntegerDatum d4v1 = new IntegerDatum(d4v0.getId(), d4v0.getVersion()+1, d4v0.getInteger()-1);
		IntegerDatum d5v1 = new IntegerDatum(d5v0.getId(), d5v0.getVersion()+1, d5v0.getInteger()+1);
		List<Update<IntegerDatum>> updates = new ArrayList<Update<IntegerDatum>>(2);
		updates.add(new Update<IntegerDatum>(d4v0, d4v1));
		updates.add(new Update<IntegerDatum>(d5v0, d5v1));
		
		partitioner.update(new ArrayList<Update<IntegerDatum>>(), updates, new ArrayList<Update<IntegerDatum>>());
		
		assertTrue(negative.getValues().containsAll(negativeData));
		assertTrue(negative.getValues().contains(d4v1));
		assertTrue(negative.getValues().size() == negativeData.size()+1);
		assertTrue(positive.getValues().containsAll(positiveData));
		assertTrue(positive.getValues().contains(d5v1));
		assertTrue(positive.getValues().size() == positiveData.size()+1);
		
		// 'amend' away - to another partition
		IntegerDatum d4v2 = new IntegerDatum(d4v1.getId(), d4v1.getVersion()+1, d4v1.getInteger()*-1);
		IntegerDatum d5v2 = new IntegerDatum(d5v1.getId(), d5v1.getVersion()+1, d5v1.getInteger()*-1);
		List<Update<IntegerDatum>> updates2 = new ArrayList<Update<IntegerDatum>>(2);
		updates2.add(new Update<IntegerDatum>(d4v1, d4v2));
		updates2.add(new Update<IntegerDatum>(d5v1, d5v2));
		
		partitioner.update(new ArrayList<Update<IntegerDatum>>(), updates2, new ArrayList<Update<IntegerDatum>>());
		
		assertTrue(negative.getValues().containsAll(negativeData));
		assertTrue(negative.getValues().contains(d5v2));
		assertTrue(negative.getValues().size() == negativeData.size()+1);
		assertTrue(positive.getValues().containsAll(positiveData));
		assertTrue(positive.getValues().contains(d4v2));
		assertTrue(positive.getValues().size() == positiveData.size()+1);

		// 'amend' away - to nowhere
		IntegerDatum d4v3 = new IntegerDatum(d4v2.getId(), d4v2.getVersion()+1, 0);
		IntegerDatum d5v3 = new IntegerDatum(d5v2.getId(), d5v2.getVersion()+1, 0);
		List<Update<IntegerDatum>> updates3 = new ArrayList<Update<IntegerDatum>>(2);
		updates3.add(new Update<IntegerDatum>(d4v2, d4v3));
		updates3.add(new Update<IntegerDatum>(d5v2, d5v3));
		
		partitioner.update(new ArrayList<Update<IntegerDatum>>(), updates3, new ArrayList<Update<IntegerDatum>>());
		
		assertTrue(!negative.getValues().contains(d4v3));
		assertTrue(!negative.getValues().contains(d5v3));
		assertTrue(negative.getValues().containsAll(negativeData));
		assertTrue(negative.getValues().size() == negativeData.size());
		assertTrue(!positive.getValues().contains(d4v3));
		assertTrue(!positive.getValues().contains(d5v3));
		assertTrue(positive.getValues().containsAll(positiveData));
		assertTrue(positive.getValues().size() == positiveData.size());
		
	}
}
