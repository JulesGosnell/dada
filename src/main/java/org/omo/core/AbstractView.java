package org.omo.core;


public abstract class AbstractView<InputKey, InputValue> implements View<InputKey, InputValue> {

	public void connect(Model<InputKey, InputValue> model) {
		synchronized (getLock()) {
			batch(model.registerView(this).getData(), null, null);
		}
	}
	
	protected abstract Object getLock();
	
}
