package org.omo.jms;

import java.io.Serializable;

public class Results implements Serializable {

	private final boolean exception;
	private final Object value;
	
	Results(boolean exception, Object result) {
		this.exception = exception;
		this.value = result;
	}

	public Object getValue() {
		return value;
	}

	public boolean isException() {
		return exception;
	}
	
	public String toString() {
		return "<"+getClass().getSimpleName()+": "+value+">";
	}
	
}
