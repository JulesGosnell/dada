/**
 * 
 */
package com.nomura.cash2;

import java.util.List;

public interface View<Input> {
	
	void upsert(List<Input> upsertions);
	void upsert(Input upsertion);
	
	void delete(List<Integer> deletions);
	void delete(int deletion);
	
}