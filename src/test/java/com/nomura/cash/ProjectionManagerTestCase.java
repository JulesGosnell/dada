package com.nomura.cash;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

public class ProjectionManagerTestCase extends TestCase {

	protected int accountId;
	protected Account account;
	protected Date start;
	protected int granularity;
	protected PartitioningStrategy<Date> strategy;
	protected List<PositionManager<Account, Trade>> managers;
	protected ProjectionManager<Account, Trade> manager;
	
	protected void setUp() throws Exception {
		super.setUp();
		accountId = 1;
		account = new AccountImpl(accountId);
		start = new Date(); //now
		granularity = 1000; // one second
		strategy = new DatePartitioningStrategy(start, granularity);
		managers = new ArrayList<PositionManager<Account,Trade>>();
		manager = new ProjectionManagerImpl<Account, Trade>(account, strategy, managers);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		manager = null;
		account = null;
		accountId = 0;
	}

	public void testProjection() {
		assertTrue(true);
	}
}
