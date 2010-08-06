package org.dada.core;


public interface SessionManager {

	@Deprecated
	String getName();
	@Deprecated
	Model<Object, Object> getModel(String name);
	
	Metadata<Object, Object> getMetadata(String modelName);
	
	Data<Object> registerView(String modelName, View<Object> view);
	Data<Object> deregisterView(String name, View<Object> view);
	
	Model<Object, Object> query(String namespace, String query);
	
}
