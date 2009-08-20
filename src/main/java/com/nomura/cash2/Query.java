/**
 * 
 */
package com.nomura.cash2;

import java.util.LinkedList;
import java.util.List;

public interface Query<T> {
	
	boolean apply(T element);
	LinkedList<T> apply(List<T> elements);
	
}