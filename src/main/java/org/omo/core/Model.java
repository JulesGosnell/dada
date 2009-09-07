package org.omo.core;

import java.util.Collection;

public interface Model<Output> extends Lifecycle {

	Collection<Output> registerView(View<Output> view);
	void deregisterView(View<Output> view);
	
}