package org.omo.cash2;

public interface Model<Output> extends Lifecycle {

	void registerView(View<Output> view);
	void deregisterView(View<Output> view);
	
}