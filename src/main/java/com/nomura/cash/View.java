package com.nomura.cash;

import java.util.List;

public interface View<T> {

	/**
	 * Called on a regular basis by a set of Models to keep View in sync.
	 * Each update() will be the delta between the Model's current state and that at the time of the last update().
	 * An initial implicit state of null is assumed, guaranteeing a full refresh with the first update().
	 * 
	 * @param additions
	 * @param removals
	 * @param modifications
	 */
	void update(Model<T> model, List<T> additions, List<Integer> removals, List<T> modifications);

}
