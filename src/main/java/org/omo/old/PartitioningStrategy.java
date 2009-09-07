package org.omo.old;

public interface PartitioningStrategy<T> {

	int getPartition(T t);
	
}
