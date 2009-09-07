package org.omo.cash;

import org.omo.cash.Identifiable;
import org.omo.cash.Position;
import org.omo.cash.PositionManagerImpl;

public class PartitionManagerImpl<I extends Identifiable, T extends Position>
		extends PositionManagerImpl<I, T> implements PartitionManager<I, T> {

	protected int startingBalance;
	
	public PartitionManagerImpl(I owner) {
		super(owner);
	}

	@Override
	public int getStartingBalance() {
		return startingBalance;
	}

	@Override
	public void setStartingBalance(int startingBalance) {
		this.startingBalance = startingBalance;
	}

}
