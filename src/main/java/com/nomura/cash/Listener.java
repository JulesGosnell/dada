package com.nomura.cash;

public interface Listener<T extends Identifiable> {

	void update(T oldValue, T newValue);
	
}
