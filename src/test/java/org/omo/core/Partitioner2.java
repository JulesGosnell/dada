/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Partitioner2<K, V> implements View<K, V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final Collection<Update<V>> EMPTY = new ArrayList<Update<V>>();
	protected final PartitionMap<K, V> partitionMap;
	
	public Partitioner2(PartitionMap<K, V> partitionMap) {
		this.partitionMap = partitionMap;
	}
	
	protected Collection<Update<V>> put(Map<K, Collection<Update<V>>> map, K key, Update<V> value, int averageSize) {
		Collection<Update<V>> collection = map.get(key);
		if (collection == null)
			map.put(key, collection = new ArrayList<Update<V>>(averageSize));
		collection.add(value);
		return collection;
	}
	
	protected Collection<Update<V>> get(Map<K, Collection<Update<V>>> map, K key) {
		Collection<Update<V>> value = map.get(key);
		return value == null ? EMPTY : value;
	}
	
	// TODO: should this be refactored to use a 1x3 rather than 3x1 approach - would save on hashing, but maybe cause more allocation ?
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		int numberOfPartitions = partitionMap.size();
		int averageSize = Math.max(Math.max(insertions.size(), updates.size()), deletions.size()) / numberOfPartitions;
		Map<K, Collection<Update<V>>> insertionsMap = new HashMap<K, Collection<Update<V>>>(numberOfPartitions);
		Map<K, Collection<Update<V>>> updatesMap = new HashMap<K, Collection<Update<V>>>(numberOfPartitions);
		Map<K, Collection<Update<V>>> deletionsMap = new HashMap<K, Collection<Update<V>>>(numberOfPartitions);

		// split the insertions into a group for each partition
		for (Update<V> insertion : insertions) {
			V newValue = insertion.getNewValue();
			K key = partitionMap.getKey(newValue);
			// this algorithm assumes that many updates may not fit any partition and so filters them out...
			if (partitionMap.containsKey(key)) {
				put(insertionsMap, key, insertion, averageSize);
			}
		}
		// split the updates into a group for each partition
		for (Update<V> update : updates) {
			V oldValue = update.getOldValue();
			V newValue = update.getNewValue();
			K oldKey = partitionMap.getKey(oldValue);
			K newKey = partitionMap.getKey(newValue);
			
			if (!oldKey.equals(newKey)) {
				if (partitionMap.containsKey(oldKey)) {
					put(deletionsMap, oldKey, update, averageSize);
				}
				if (partitionMap.containsKey(newKey)) {
					put(insertionsMap, newKey, update, averageSize);
				}
			} else {
				if (partitionMap.containsKey(newKey)) {
					put(updatesMap, newKey, update, averageSize);
				}
			}
		}
		// split the deletions into a group for each partition
		for (Update<V> deletion : deletions) {
			V oldValue = deletion.getOldValue();
			K key = partitionMap.getKey(oldValue);
			// this algorithm assumes that many updates may not fit any partition and so filters them out...
			if (partitionMap.containsKey(key)) {
				put(deletionsMap, key, deletion, averageSize);
			}
		}
		// calculate set of partitions for which we have changes...
		Set<K> keys = new HashSet<K>(numberOfPartitions);
		keys.addAll(insertionsMap.keySet());
		keys.addAll(updatesMap.keySet());
		keys.addAll(deletionsMap.keySet());
		// dispatch changes onto their respective partitions 
		for (K key : keys) {
			View<K, V> partition = partitionMap.getPartition(key);
			if (partition == null) {
				logger.error("no partition for key: {}", key);
			} else {
				partition.update(get(insertionsMap, key), get(updatesMap, key), get(deletionsMap, key));
			}
		}
	}
	
}