package com.nomura.cash;

import junit.framework.TestCase;

public class PositionManagerTestCase extends TestCase {

	// aggregate Trades by Currency
	protected int currencyId = 0;
	protected Currency currency = new CurrencyImpl(currencyId);
	protected CurrencyManager currencyManager = new CurrencyManagerImpl(currency);
	// aggregate Trades by Account
	protected int account1Id = 1;
	protected Account account1 = new AccountImpl(account1Id);
	protected AccountManager account1Manager  = new AccountManagerImpl(account1);
	// aggregate Trades by Account
	protected int account2Id = 2;
	protected Account account2 = new AccountImpl(account2Id);
	protected AccountManager account2Manager  = new AccountManagerImpl(account2);
	protected int trade1Id = 3;
	protected int trade1Amount = 30;
	protected Trade trade1 = new TradeImpl(trade1Id, trade1Amount);
	protected int trade2Id = 4;
	protected int trade2Amount = 40;
	protected Trade trade2 = new TradeImpl(trade2Id, trade2Amount);
	
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
	}
	

}
