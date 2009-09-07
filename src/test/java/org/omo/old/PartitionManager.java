package org.omo.old;

import org.omo.old.Identifiable;
import org.omo.old.Position;
import org.omo.old.PositionManager;

public interface PartitionManager<I extends Identifiable, T extends Position> extends PositionManager<I, T> {

	int getStartingBalance();
	void setStartingBalance(int startingBalance);
	
}
