package org.omo.cash;

/**
 * A Projection is an initialBalance aggregated with a series of Positions.
 * 
 * @author jules
 *
 * @param <I>
 * @param <T>
 */
public interface ProjectionManager<I extends Identifiable, P extends Position, T extends PositionManager<I, P>> extends PositionManager<I, T> {
	
	int getInitialBalance();
	void setInitialBalance(int initialBalance);
}
