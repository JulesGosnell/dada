/**
 * 
 */
package com.nomura.cash2;

import java.util.List;

public interface Listener<T> {
	
	void upsert(List<T> upsertions);
	void upsert(T upsertion);
	
	void delete(List<Integer> deletions);
	void delete(int deletion);
	
}