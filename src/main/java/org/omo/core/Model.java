package org.omo.core;

public interface Model<OutputKey, OutputValue> extends Lifecycle {

	String getName();
	
	Registration<OutputKey, OutputValue> registerView(View<OutputKey, OutputValue> view);
	boolean deregisterView(View<OutputKey, OutputValue> view);
	
}