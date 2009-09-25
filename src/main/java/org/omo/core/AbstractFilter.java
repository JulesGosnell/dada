/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;

public abstract class AbstractFilter<V> implements Filter<V> {
	
	@Override
	public LinkedList<V> apply(Collection<V> values) {
		LinkedList<V> results = new LinkedList<V>();
		for (V element : values)
			if (apply(element))
				results.addFirst(element);
		return results;
	}
}