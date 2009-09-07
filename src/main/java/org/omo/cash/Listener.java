package org.omo.cash;

public interface Listener<T> {

	void update(T oldValue, T newValue);
	
}
