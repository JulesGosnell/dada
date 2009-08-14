package com.nomura.cash;

public class PositionManagerImpl<I extends Identifiable, T extends Position> extends ManagerImpl<I, T> implements PositionManager<I, T> {

	protected final PositionAggregator<T> aggregator = new PositionAggregator<T>();
	
	protected boolean excluded;
	
	public PositionManagerImpl(I identity) {
		super(identity);
		register(aggregator);
	}

	@Override
	public int getPosition() {
		return aggregator.getAggregate();
	}
	
	@Override
	public boolean getExcluded() {
		return excluded;
	}

	@Override
	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}
}
