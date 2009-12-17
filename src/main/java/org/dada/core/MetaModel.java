package org.dada.core;

public interface MetaModel extends Model<String, String> {

	Registration<Object, Object> registerView(String modelName, View<Object, Object> view);
	boolean deregisterView(String modelName, View<Object, Object> view);
	
}
