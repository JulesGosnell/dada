/**
 * 
 */
package org.omo.core;

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

}