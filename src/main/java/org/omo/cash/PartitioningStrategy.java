package org.omo.cash;

public interface PartitioningStrategy<T> {

	int getPartition(T t);
	
}
