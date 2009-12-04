package org.omo.core;

public abstract class AbstractRange<V> implements Range<V> {

	protected final V min;
	protected final V max;
	
	public AbstractRange(V min, V max) {
		this.min = min;
		this.max = max;
	}

	public V getMin() {
		return min;
	}
	
	public V getMax() {
		return max;
	}
}
