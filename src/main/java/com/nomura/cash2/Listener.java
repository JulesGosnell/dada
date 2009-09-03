/**
 * 
 */
package com.nomura.cash2;

import java.util.List;

public interface Listener<T> {
	
	interface Key{};
	
	void upsert(List<T> upsertions);
	void upsert(T upsertion);
	
	void delete(List<Key> deletions);
	void delete(Key deletion);
	
}