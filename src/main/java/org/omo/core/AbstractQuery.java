/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;

public abstract class AbstractQuery<T> implements Query<T> {
	
	@Override
	public LinkedList<T> apply(Collection<T> elements) {
		LinkedList<T> results = new LinkedList<T>();
		for (T element : elements)
			if (apply(element))
				results.addFirst(element);
		return results;
	}
}