/**
 * 
 */
package org.omo.cash2;

class IdentityFilter<T> extends AbstractQuery<T> {

	@Override
	public boolean apply(T element) {
		return true;
	}

}