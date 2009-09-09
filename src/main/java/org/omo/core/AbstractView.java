package org.omo.core;


public abstract class AbstractView<InputKey, InputValue> implements View<InputKey, InputValue> {

	public void connect(Model<InputKey, InputValue> model) {
		synchronized (getLock()) {
			upsert(model.registerView(this));
		}
	}
	
	protected abstract Object getLock();
	
}
