/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<Input> {
	
	void upsert(Collection<Input> upsertions);
	void upsert(Input upsertion);
	
	void delete(Collection<Integer> deletions);
	void delete(int deletion);
	
}