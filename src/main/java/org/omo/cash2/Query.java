/**
 * 
 */
package org.omo.cash2;

import java.util.Collection;
import java.util.LinkedList;

public interface Query<T> {
	
	boolean apply(T element);
	LinkedList<T> apply(Collection<T> elements);
	
}