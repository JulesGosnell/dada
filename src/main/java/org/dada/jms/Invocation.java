package org.omo.jms;

import java.io.Serializable;

public class Invocation implements Serializable {

	private final int methodIndex;
	private final Object[] args;
	
	public Invocation(int methodIndex, Object[] args) {
		this.methodIndex = methodIndex;
		this.args = args;
	}
	
	public int getMethodIndex() {
		return methodIndex;
	}

	public Object[] getArgs() {
		return args;
	}
}
