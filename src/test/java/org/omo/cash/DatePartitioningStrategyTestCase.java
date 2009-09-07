package org.omo.cash;

import java.util.Date;

import org.omo.cash.DatePartitioningStrategy;

import junit.framework.TestCase;

public class DatePartitioningStrategyTestCase extends TestCase {

	final long DAY = 24*60*60*1000;
	final long WEEK = 7*DAY;

	public void testDates() {
		Date now = new Date();
		Date NEXT_WEEK = new Date(now.getTime()+WEEK);
		Date THIS_WEEK = new Date(NEXT_WEEK.getTime()-WEEK);
		assertTrue(THIS_WEEK.equals(now));
	}
	
	public void testPartitions() {
		Date start = new Date();
		DatePartitioningStrategy strategy = new DatePartitioningStrategy(start, DAY);
		
		assertTrue(strategy.getPartition(new Date(start.getTime()+(0*DAY))) == 0);
		assertTrue(strategy.getPartition(new Date(start.getTime()+(1*DAY))) == 1);
		assertTrue(strategy.getPartition(new Date(start.getTime()+(2*DAY))) == 2);
		assertTrue(strategy.getPartition(new Date(start.getTime()+(3*DAY))) == 3);
		assertTrue(strategy.getPartition(new Date(start.getTime()+(4*DAY))) == 4);
		assertTrue(strategy.getPartition(new Date(start.getTime()+(5*DAY))) == 5);
		assertTrue(strategy.getPartition(new Date(start.getTime()+(6*DAY))) == 6);

	}
}
