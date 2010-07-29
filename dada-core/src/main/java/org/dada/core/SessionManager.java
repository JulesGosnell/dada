package org.dada.core;

import java.util.Collection;

public interface SessionManager {

	@Deprecated
	String getName();
	
	@Deprecated
	Model<Object, Object> getModel(String name);
	
	Registration<Object, Object> registerView(String modelName, View<Object> view);
	Collection<Object> deregisterView(String modelName, View<Object> view);

	Registration<Object, Object> registerQueryView(String query, View<Object> view);
	Collection<Object> deregisterQueryView(String query, View<Object> view);
	
}
