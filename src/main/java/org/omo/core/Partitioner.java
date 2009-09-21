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
	public void insert(V value) {
		partitions.get(strategy.partition(value)).insert(value);
	}

	@Override
	public void update(V value) {
		partitions.get(strategy.partition(value)).update(value);
	}

	@Override
	public void delete(K key) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void batch(Collection<V> insertions, Collection<V> updates, Collection<K> deletions) {
		int numberofPartitions = strategy.getNumberOfPartitions();
		List<V>[] tmp = new List[numberofPartitions];
		for (int p=0; p<numberofPartitions; p++)
			tmp[p] = new ArrayList<V>();
		for (V insertion : insertions)
			tmp[strategy.partition(insertion)].add(insertion);
		for (int p=0; p<numberofPartitions; p++) {
			View<K, V> partition = partitions.get(p);
			List<V> insertions2 = tmp[p];
			if (insertions2.size()>0)
				partition.batch(insertions2, null, null);
			// TODO: extend
		}
	}

}

