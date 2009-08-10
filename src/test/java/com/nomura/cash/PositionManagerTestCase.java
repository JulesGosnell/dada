package com.nomura.cash;

import junit.framework.TestCase;

public class PositionManagerTestCase extends TestCase {

	protected int currencyId = 0;
	protected Position currency = new PositionImpl(currencyId, 0);
	protected PositionManager<Position, Position> currencyManager = new PositionManagerImpl<Position, Position>(currency);
	protected int account1Id = 1;
	protected Position account1 = new PositionImpl(account1Id, 0);
	protected PositionManager<Position, Position> account1Manager  = new PositionManagerImpl<Position, Position>(account1);
	protected int account2Id = 2;
	protected Position account2 = new PositionImpl(account2Id, 0);
	protected PositionManager<Position, Position> account2Manager  = new PositionManagerImpl<Position, Position>(account2);
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testAddTrade() {
		int trade1Id = 0;
		int trade1Amount = 10;
		Trade trade1 = new TradeImpl(trade1Id, account1Id, currencyId, trade1Amount);
		currencyManager.update(trade1);
		account1Manager.update(trade1);

		int trade2Id = 0;
		int trade2Amount = 10;
		Trade trade2 = new TradeImpl(trade2Id, account2Id, currencyId, trade2Amount);

		assertTrue(currencyManager.getPosition() == trade1Amount+trade2Amount);
		assertTrue(account1Manager.getPosition() == trade1Amount);
		assertTrue(account2Manager.getPosition() == trade2Amount);
	}
	

}
