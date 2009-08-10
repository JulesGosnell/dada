package com.nomura.cash;

import java.util.List;

public interface Listener<T extends Identifiable> {

	void update(T oldValue, T newValue);
	
}
