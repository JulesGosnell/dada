package com.nomura.cash2;

import java.util.ArrayList;
import java.util.List;

public class AbstractModel<Output> implements Model<Output> {

	protected final List<View<Output>> views = new ArrayList<View<Output>>();
	
	@Override
	public void registerView(View<Output> view) {
		views.add(view);
	}
	
	@Override
	public void deregisterView(View<Output> view) {
		views.remove(view);
	}
	
}
