package org.dada.core;

public class ViewNameGetter implements  Getter<String,View<?>> {

	@Override
	public String get(View<?> value) {
		return "View-" + System.identityHashCode(value);
	}

}
