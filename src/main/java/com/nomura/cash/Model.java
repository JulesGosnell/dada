package com.nomura.cash;

import java.util.List;

public interface Model<T> {

	/**
	 * Attach an asynchronous View to this Model.
	 * The View will consist of the intersection of the Model's dataset and that described by the Ids passsed.
	 * The View will receive regular update()s on the state of this intersection.
	 * 
	 * @param client
	 * @param ids
	 * @return
	 */
	public abstract List<T> view(View<T> client, List<Integer> ids);

}