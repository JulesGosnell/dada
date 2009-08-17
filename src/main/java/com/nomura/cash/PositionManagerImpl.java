package com.nomura.cash;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// TODO - what if position changes arrive in the wrong order ?
// can we just check the oldValue against our current value...?

public class PositionManagerImpl<I extends Identifiable, T extends Position> extends ManagerImpl<I, T> implements PositionManager<I, T> {

	protected final AtomicInteger position = new AtomicInteger(0);

	protected final List<Listener<Integer>> positionListeners = new ArrayList<Listener<Integer>>();

	protected final Listener<T> aggregator = new Listener<T>() {	

		@Override
		public void update(T oldValue, T newValue) {
			int delta = (oldValue==null || oldValue.getExcluded() ? 0 :oldValue.getPosition())-(newValue.getExcluded() ? 0 : newValue.getPosition());
			int oldPosition = position.getAndAdd(-delta); // aggregate -= delta - atomic change..
			int newPosition = oldPosition+delta;
			for (Listener<Integer> listener : positionListeners)
				listener.update(oldPosition, newPosition);
		}
	};
	
	protected boolean excluded;
	
	public PositionManagerImpl(I identity) {
		super(identity);
		register(aggregator);
	}

	@Override
	public int getPosition() {
		return position.get();
	}
	
	@Override
	public boolean getExcluded() {
		return excluded;
	}

	@Override
	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

	@Override
	public void deregisterPositionListener(Listener<Integer> listener) {
		positionListeners.add(listener);
		
	}

	@Override
	public void registerPositionListener(Listener<Integer> listener) {
		positionListeners.remove(listener);
		
	}
}
