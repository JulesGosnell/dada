package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Partitioner<K, V extends Datum> implements View<K, V> {

	public interface Strategy<V> {
		
		int getNumberOfPartitions();
		int partition(V value);
		
	}
	
	private final List<View<K, V>> partitions;
	private final Strategy<V> strategy;
	
	public Partitioner(List<View<K, V>> partitions, Strategy<V> strategy) {
		this.partitions = partitions;
		this.strategy = strategy;
	}
	
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		int numberofPartitions = strategy.getNumberOfPartitions();
		List<Update<V>>[] tmp = new List[numberofPartitions];
		for (int p=0; p<numberofPartitions; p++)
			tmp[p] = new ArrayList<Update<V>>();
		for (Update<V> insertion : insertions) {
			V newValue = insertion.getNewValue();
			tmp[strategy.partition(newValue)].add(new Update<V>(null, newValue));
		}
		for (int p=0; p<numberofPartitions; p++) {
			View<K, V> partition = partitions.get(p);
			List<Update<V>> insertions2 = tmp[p];
			if (insertions2.size()>0)
				partition.update(insertions2, new ArrayList<Update<V>>(), new ArrayList<Update<V>>());
		}
	}

}

