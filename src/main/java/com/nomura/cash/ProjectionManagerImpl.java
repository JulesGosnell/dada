package com.nomura.cash;

import java.util.Date;
import java.util.List;

public class ProjectionManagerImpl<I extends Identifiable, P extends Position, T extends PositionManager<I, P>>
		extends PositionManagerImpl<I, T> implements ProjectionManager<I, P, T> {

	protected final PartitioningStrategy<Date> strategy;
	protected int initialBalance;
	
	public ProjectionManagerImpl(I owner, PartitioningStrategy<Date> strategy) {
		super(owner);
		this.strategy = strategy;
	}

	@Override
	public int getInitialBalance() {
		return initialBalance;
	}

	@Override
	public void setInitialBalance(int initialBalance) {
		int delta = initialBalance - this.initialBalance;
		this.initialBalance = initialBalance;
		// recalculateProjection(delta);
	}
	
	// from 'Manager'
	@Override
	public void update(List<T> updates) {
		if (updates.size()>0)
			for (T update: updates)
				update(update);
	}

	protected Listener<Integer> positionListener = new Listener<Integer>() {
		
		@Override
		public void update(Integer oldValue, Integer newValue) {
			// TODO Auto-generated method stub
			
		}
	};
	
	@Override
	public void update(T newValue) {
		T oldValue = id2T.put(newValue.getId(), newValue);
		// TODO - position should only receive one update here - not two...
		newValue.registerPositionListener(positionListener);
		if (oldValue!=null) oldValue.deregisterPositionListener(positionListener);
		for (Listener<T> listener : listeners)
			listener.update(oldValue, newValue);
	}
	
}
