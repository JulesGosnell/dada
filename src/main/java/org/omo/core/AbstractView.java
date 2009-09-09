package org.omo.core;


public abstract class AbstractView<Input> implements View<Input> {

	public void connect(Model<Input> model) {
		synchronized (getLock()) {
			upsert(model.registerView(this));
		}
	}
	
	protected abstract Object getLock();
	
}
