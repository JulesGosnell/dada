package com.nomura.cash2;

interface View<T> extends Listener <T> {

	public void addElementListener(Listener<T> listener);

	public void removeElementListener(Listener<T> listener);

}