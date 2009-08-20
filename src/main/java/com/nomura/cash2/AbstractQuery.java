/**
 * 
 */
package com.nomura.cash2;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractQuery<T> implements Query<T> {
	
	@Override
	public LinkedList<T> apply(List<T> elements) {
		LinkedList<T> results = new LinkedList<T>();
		for (T element : elements)
			if (apply(element))
				results.addFirst(element);
		return results;
	}
}