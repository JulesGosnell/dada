package com.nomura.cash;

public class PositionManagerImpl<I extends Identifiable, T extends Position> extends ManagerImpl<I, T> implements PositionManager<I, T> {

	protected final PositionAggregator<T> aggregator = new PositionAggregator<T>();
	
	public PositionManagerImpl(I identity) {
		super(identity);
		register(aggregator);
	}

	@Override
	public int getPosition() {
		return aggregator.getAggregate();
	}
}
