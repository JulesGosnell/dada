/**
 * 
 */
package org.omo.core;

public class IdentityFilter<V> implements Filter<V> {

	@Override
	public boolean apply(V value) {
		return true;
	}

}