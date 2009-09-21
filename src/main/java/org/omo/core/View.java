/**
 * 
 */
package org.omo.core;

import java.util.Collection;

public interface View<InputKey, InputValue> {
	 
	void batch(Collection<InputValue> insertions, Collection<InputValue> updates, Collection<InputKey> deletions);
	
	void insert(InputValue value);
	void update(InputValue value);
	void delete(InputKey key);
	
}