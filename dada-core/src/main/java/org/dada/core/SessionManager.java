package org.dada.core;

import java.util.Collection;

public interface SessionManager {

	@Deprecated
	String getName();
	@Deprecated
	Model<Object, Object> getModel(String name);
	Data<Object> getData(String name);
	
	Metadata<Object, Object> getMetadata(String modelName);
	
	Data<Object> registerView(Model<Object, Object> model, View<Object> view);
	Data<Object> deregisterView(Model<Object, Object> model, View<Object> view);
    Model<Object, Object> find(Model<Object, Object> model, Object key);
    
    Collection<Object> query(String namespace, String query);
}
