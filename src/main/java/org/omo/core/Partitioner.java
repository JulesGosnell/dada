package org.omo.core;

import java.util.Collection;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;


public class Partitioner<K, V extends Datum<Integer>> extends Router<Integer, K, V>  {

	public static class PartitioningStrategy<K,V extends Datum<Integer>> implements Strategy<Integer, K, V> {

		private final Collection<View<K, V>>[] views;
		private final int numPartitions;
		
		public PartitioningStrategy(Collection<View<K, V>> partitions) {
			numPartitions = partitions.size();
			views = new Collection[numPartitions];
			int i = 0;
			for (View<K, V> partition : partitions)
				views[i++] = Collections.singleton(partition);
		}
		
		@Override
		public boolean getMutable() {
			return false;
		}

		@Override
		public Integer getRoute(V value) {
			return value.getId() % numPartitions;
		}

		@Override
		public Collection<View<K, V>> getViews(Integer route) {
			return views[route];
		}
		
	}
	
	public Partitioner(List<View<K, V>> partitions) {
		super(new PartitioningStrategy<K, V>(partitions));
	}
}

