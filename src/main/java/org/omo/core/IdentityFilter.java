/**
 * 
 */
package org.omo.core;

class IdentityFilter<T> extends AbstractQuery<T> {

	@Override
	public boolean apply(T element) {
		return true;
	}

}