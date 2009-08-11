package com.nomura.cash;

import junit.framework.TestCase;

public class PositionManagerTestCase extends TestCase {

	// aggregate Trades by Currency
	protected int currencyId = 0;
	protected Position currency = new PositionImpl(currencyId, 0);
	protected PositionManager<Position, Position> currencyManager = new PositionManagerImpl<Position, Position>(currency);
	// aggregate Trades by Account
	protected int account1Id = 1;
	protected Position account1 = new PositionImpl(account1Id, 0);
	protected PositionManager<Position, Position> account1Manager  = new PositionManagerImpl<Position, Position>(account1);
	// aggregate Trades by Account
	protected int account2Id = 2;
	protected Position account2 = new PositionImpl(account2Id, 0);
	protected PositionManager<Position, Position> account2Manager  = new PositionManagerImpl<Position, Position>(account2);
	protected int trade1Id = 3;
	protected int trade1Amount = 30;
	protected Position trade1 = new PositionImpl(trade1Id, trade1Amount);
	protected int trade2Id = 4;
	protected int trade2Amount = 40;
	protected Position trade2 = new PositionImpl(trade2Id, trade2Amount);
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testAddTrade() {
		currencyManager.update(trade1);
		account1Manager.update(trade1);
		currencyManager.update(trade2);
		account2Manager.update(trade2);
		
		int tradeTotal = trade1Amount+trade2Amount;
		assertTrue(currencyManager.getPosition() == tradeTotal);
		assertTrue(account1Manager.getPosition() == trade1Amount);
		assertTrue(account2Manager.getPosition() == trade2Amount);
		
		// TODO - doesn't work..
		// assertTrue(currencyByAccount.getPosition() == tradeTotal);
		
		// what should currencyByAccount be aggregating - accounts or accountmanagers...
		// accounts are small but not live
		// accountManagers are live but to big to refresh with idempotent events
		
		// needs a rethink...
		
		
	}
	

}
