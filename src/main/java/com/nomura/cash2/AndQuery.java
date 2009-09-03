/**
 * 
 */
package com.nomura.cash2;

import java.util.Collection;
import java.util.LinkedList;

public class AndQuery<T> implements Query<T> {
	
	Query<T> lhs;
	Query<T> rhs;
	
	AndQuery(Query<T> lhs, Query<T> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	@Override
	public boolean apply(T element) {
		return lhs.apply(element) && rhs.apply(element);
	}

	@Override
	public LinkedList<T> apply(Collection<T> elements) {
		return rhs.apply(lhs.apply(elements));
	}

}