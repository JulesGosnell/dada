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
	public void delete(Collection<K> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(K deletion) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void upsert(Collection<V> upsertions) {
		int numberofPartitions = strategy.getNumberOfPartitions();
		List<V>[] tmp = new List[numberofPartitions];
		for (int p=0; p<numberofPartitions; p++)
			tmp[p] = new ArrayList<V>();
		for (V upsertion : upsertions)
			tmp[strategy.partition(upsertion)].add(upsertion);
		for (int p=0; p<numberofPartitions; p++) {
			View<K, V> partition = partitions.get(p);
			List<V> upsertions2 = tmp[p];
			if (upsertions2.size()>0)
				partition.upsert(upsertions2);
		}
	}

	@Override
	public void upsert(V upsertion) {
		partitions.get(strategy.partition(upsertion)).upsert(upsertion);
	}

}

