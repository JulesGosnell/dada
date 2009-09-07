package org.omo.cash;

public class TradeManagerImpl extends PositionManagerImpl<Identifiable, Trade> implements TradeManager {
	
	public TradeManagerImpl(Identifiable identity) {
		super(identity);
	}
}
