package org.omo.old;

import java.io.Serializable;

public class Update<T extends Serializable> implements Serializable {
	
	protected final T update;
	
	public Update(T update) {
		this.update = update; 
	}

	public T getUpdate() {
		return update;
	}
}
