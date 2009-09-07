package org.omo.cash;

import org.omo.cash.Identifiable;
import org.omo.cash.Position;
import org.omo.cash.PositionManager;

public interface PartitionManager<I extends Identifiable, T extends Position> extends PositionManager<I, T> {

	int getStartingBalance();
	void setStartingBalance(int startingBalance);
	
}
