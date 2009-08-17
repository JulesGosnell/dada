package com.nomura.cash;

public interface PartitionManager<I extends Identifiable, T extends Position> extends PositionManager<I, T> {

	int getStartingBalance();
	void setStartingBalance(int startingBalance);
	
}
