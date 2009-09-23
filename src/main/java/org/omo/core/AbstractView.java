package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


public abstract class AbstractView<K, V> implements View<K, V> {

	public void connect(Model<K, V> model) {
		synchronized (getLock()) {
			batch(model.registerView(this).getData(), new ArrayList<Update<V>>(), new ArrayList<K>());
		}
	}
	
	protected abstract Object getLock();
	
}
