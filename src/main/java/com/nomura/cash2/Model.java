package com.nomura.cash2;

public interface Model<Output> {

	public void registerView(View<Output> view);
	public void deregisterView(View<Output> view);

}