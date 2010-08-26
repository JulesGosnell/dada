package org.dada.core;

import java.io.Serializable;

import clojure.lang.Indexed;

// e.g.
//[tgt-metamodel new-prefix extra-pairs :count]
// add inputs

public class Result implements Serializable, Indexed {

	private final Model<?, ?> model;
	private final Object prefix;
	private final Object pairs;
	private final Object operation;
	
	public Result(Model<?, ?> model, Object prefix, Object pairs, Object operation) {
		super();
		this.model = model;
		this.prefix = prefix;
		this.pairs = pairs;
		this.operation = operation;
	}

	public Model<?, ?> getModel() {
		return model;
	}

	public Object getPrefix() {
		return prefix;
	}

	public Object getPairs() {
		return pairs;
	}

	public Object getOperation() {
		return operation;
	}

	// Indexed
	
	@Override
	public int count() {
		return 4;
	}

	@Override
	public Object nth(int i) {
		return nth(i, null);
	}

	@Override
	public Object nth(int i, Object notFound) {
		switch (i) {
		case 0: return model;
		case 1: return prefix;
		case 2: return pairs;
		case 3: return operation;
		default: return notFound;
		}
	}

}
