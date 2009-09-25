/**
 * 
 */
package org.omo.core;

class IdentityFilter<V> implements Filter<V> {

	@Override
	public boolean apply(V value) {
		return true;
	}

}