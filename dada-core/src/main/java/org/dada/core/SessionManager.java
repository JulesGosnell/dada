package org.dada.core;


public interface SessionManager {

	@Deprecated
	String getName();
	@Deprecated
	Model<Object, Object> getModel(String name);
	
	Metadata<Object, Object> getMetadata(String modelName);
	
	Registration<Object, Object> registerView(String modelName, View<Object> view);
	Deregistration<Object> deregisterView(String name, View<Object> view);

	Registration<Object, Object> registerQueryView(String query, View<Object> view);
	Deregistration<Object> deregisterQueryView(String query, View<Object> view);
	
}
