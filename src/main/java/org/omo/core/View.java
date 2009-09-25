/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<K, V> {
	 
	//void update(V value);
	void update(Collection<V> updates);
	
}