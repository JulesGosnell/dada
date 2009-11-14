/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<K, V> {
	 
	/**
	 * An insertion is a potential move from outside into a View and a deletion vice versa.
	 * An update is a new version with the same id/key.
	 * We provide the old and new version, so that the Partitioner can notify the correct partitions and a View
	 * can display a Datum that has move out, using its last known value (so you can link to its new View and follow the trail).
	 * 
	 * @param insertions
	 * @param updates
	 * @param deletions
	 */
	void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions);
	// TODO: should I put all of these in one structure ?
}

