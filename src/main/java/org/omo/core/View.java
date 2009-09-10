/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<InputKey, InputValue> {
	
	void upsert(Collection<InputValue> upsertions);
	void upsert(InputValue upsertion);
	
	void delete(Collection<InputKey> deletions);
	void delete(InputKey deletion);
	
}