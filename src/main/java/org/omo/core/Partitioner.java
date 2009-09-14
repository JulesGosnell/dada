package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Partitioner<K, V extends Datum> implements View<K, V> {

	private final List<View<K, V>> partitions;
	private final int numPartitions;
	
	public Partitioner(List<View<K, V>> partitions) {
		this.partitions = partitions;
		numPartitions = this.partitions.size();
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
		List<V>[] tmp = new List[numPartitions];
		for (int p=0; p<numPartitions; p++)
			tmp[p] = new ArrayList<V>();
		for (V upsertion : upsertions)
			tmp[upsertion.getId()%numPartitions].add(upsertion);
		for (int p=0; p<numPartitions; p++) {
			View<K, V> partition = partitions.get(p);
			List<V> upsertions2 = tmp[p];
			if (upsertions2.size()>0)
				partition.upsert(upsertions2);
		}
	}

	@Override
	public void upsert(V upsertion) {
		int partition = upsertion.getId()%numPartitions;
		partitions.get(partition).upsert(upsertion);
	}

}

