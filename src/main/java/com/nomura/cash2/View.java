package com.nomura.cash2;

public interface View<T> extends Listener <T> {

	void addElementListener(Listener<T> listener);
	void removeElementListener(Listener<T> listener);

}