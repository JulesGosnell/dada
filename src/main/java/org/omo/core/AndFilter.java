/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.LinkedList;

public class AndFilter<V> implements Filter<V> {
	
	Filter<V> lhs;
	Filter<V> rhs;
	
	AndFilter(Filter<V> lhs, Filter<V> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	@Override
	public boolean apply(V value) {
		return lhs.apply(value) && rhs.apply(value);
	}

	@Override
	public LinkedList<V> apply(Collection<V> values) {
		return rhs.apply(lhs.apply(values));
	}

}