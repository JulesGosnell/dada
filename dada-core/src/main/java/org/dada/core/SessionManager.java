package org.dada.core;

import java.util.Collection;

public interface SessionManager {

	@Deprecated
	String getName();
	@Deprecated
	Model<Object, Object> getModel(String name);
	Data<Object> getData(String name);
	
	Metadata<Object, Object> getMetadata(String modelName);
	
	Data<Object> registerView(String modelName, View<Object> view);
	Data<Object> deregisterView(String name, View<Object> view);
	
    Collection<Object> query(String namespace, String query);
	
    Model<Object, Object> find(String modelName, Object key);
	
}
