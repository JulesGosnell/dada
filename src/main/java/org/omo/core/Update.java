/**
 * 
 */
package org.omo.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class Update<V> implements Serializable {
	
	private final V oldValue;
	private final V newValue;
	
	public Update(V oldValue, V newValue) {
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public V getOldValue() {
		return oldValue;
	}

	public V getNewValue() {
		return newValue;
	}
}