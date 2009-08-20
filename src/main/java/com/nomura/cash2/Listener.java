/**
 * 
 */
package com.nomura.cash2;

import java.util.List;

public interface Listener<T> {
	
	void update(List<T> updates);
	void update(T update);
	
}