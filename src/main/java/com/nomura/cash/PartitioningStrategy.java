package com.nomura.cash;

public interface PartitioningStrategy<T> {

	int getPartition(T t);
	
}
