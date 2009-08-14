package com.nomura.cash;

import java.util.Date;
import java.util.List;


public class ProjectionManagerImpl<I extends Identifiable, T extends Position>
		extends ManagerImpl<I, T> implements ProjectionManager<I, T> {

	protected final PartitioningStrategy<Date> strategy;
	protected final List<PositionManager<I, T>> managers;
	
	public ProjectionManagerImpl(I owner, PartitioningStrategy<Date> strategy, List<PositionManager<I, T>> managers) {
		super(owner);
		this.strategy = strategy;
		this.managers = managers;
	}
}
