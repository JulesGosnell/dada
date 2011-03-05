package org.dada.core;

public interface NewSessionManager<K, V> {

	V find(String modelName, K key);
	
	Data<V> attach(String modelName, View<V> view);
	Data<V> detach(String modelName, View<V> view);
	
}
