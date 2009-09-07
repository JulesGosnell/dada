package org.omo.old;

public interface Listener<T> {

	void update(T oldValue, T newValue);
	
}
