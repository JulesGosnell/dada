package org.omo.cash;

import java.util.Date;

import org.omo.cash.Account;
import org.omo.cash.AccountImpl;
import org.omo.cash.DatePartitioningStrategy;
import org.omo.cash.PartitioningStrategy;
import org.omo.cash.PositionManager;
import org.omo.cash.ProjectionManager;
import org.omo.cash.ProjectionManagerImpl;
import org.omo.cash.Trade;

import junit.framework.TestCase;

public class ProjectionManagerTestCase extends TestCase {

	protected int accountId;
	protected Account account;
	protected Date start;
	protected int granularity;
	protected PartitioningStrategy<Date> strategy;
	protected ProjectionManager<Account, Trade, PositionManager<Account, Trade>> manager;
	
	protected void setUp() throws Exception {
		super.setUp();
		accountId = 1;
		account = new AccountImpl(accountId);
		start = new Date(); //now
		granularity = 1000; // one second
		strategy = new DatePartitioningStrategy(start, granularity);
		int numPartitions = 3;
		manager = new ProjectionManagerImpl<Account, Trade, PositionManager<Account, Trade>>(account, strategy);
		for (int i=0; i<3 ; i++) {
			Date date = new Date(start.getTime()+(granularity*i)); // TODO - use
			PartitionManager<Account, Trade> partition = new PartitionManagerImpl<Account, Trade>(account);
			manager.update(partition);
		}

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
