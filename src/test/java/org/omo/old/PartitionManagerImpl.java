package org.omo.old;


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
