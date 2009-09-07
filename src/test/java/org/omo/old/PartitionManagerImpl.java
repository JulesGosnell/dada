package org.omo.old;

import org.omo.old.Identifiable;
import org.omo.old.Position;
import org.omo.old.PositionManagerImpl;

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
