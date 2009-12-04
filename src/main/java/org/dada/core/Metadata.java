/**
 * 
 */
package org.omo.core;

import java.io.Serializable;
import java.util.List;

public interface Metadata<K, V> extends Serializable {
	
	K getKey(V value);
	Object getAttributeValue(V value, int index);
	List<String> getAttributeNames();

}