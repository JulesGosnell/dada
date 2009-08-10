/**
 * 
 */
package com.nomura.cash;

import java.util.concurrent.atomic.AtomicInteger;

public class PositionAggregator<T extends Position>implements Aggregator<Integer, T> {

	protected final AtomicInteger aggregate = new AtomicInteger(0);

	public Integer getAggregate() {
		return aggregate.get();
	}

	@Override
	public void update(Position oldValue, Position newValue) {
		int delta = (oldValue==null || oldValue.getExcluded() ? 0 :oldValue.getPosition())-(newValue.getExcluded() ? 0 : newValue.getPosition());
		aggregate.addAndGet(-delta); // aggregate -= delta
	}
}