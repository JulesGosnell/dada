/**
 * 
 */
package org.omo.core;

class IdentityFilter<V> extends AbstractFilter<V> {

	@Override
	public boolean apply(V value) {
		return true;
	}

}