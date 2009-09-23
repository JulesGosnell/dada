package org.omo.core;

public interface Model<K, V> extends Lifecycle {

	String getName();
	
	Registration<K, V> registerView(View<K, V> view);
	boolean deregisterView(View<K, V> view);
	
}