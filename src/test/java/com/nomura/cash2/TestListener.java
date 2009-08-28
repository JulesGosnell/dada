package com.nomura.cash2;

import java.io.Serializable;
import java.util.List;

public class TestListener<T> implements Listener<T>, Serializable {

	@Override
	public void update(List<T> updates) {
		System.out.println("TEST LISTENER: UPDATE()");
	}

	@Override
	public void update(T update) {
		System.out.println("TEST LISTENER: UPDATE(List)");
	}

}
