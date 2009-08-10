package com.nomura.cash;
import junit.framework.TestCase;


// TODO - push Aggregator back into Manager ?

public class ManagerTestCase extends TestCase {

	protected Identifiable identity;
	protected Manager<Identifiable, Position> manager;

	public void setUp() throws Exception {
		identity = new IdentifiableImpl(0);
		manager = new ManagerImpl<Identifiable, Position>(identity);
	}

	public void tearDown() throws Exception {
		manager = null;
	}

	public void testAggregator() {
		final int id=0;
		assertTrue(manager.size()==0);
		PositionImpl position = new PositionImpl(id, 10);
		PositionAggregator aggregator = new PositionAggregator();

		manager.register(aggregator);
		assertTrue(aggregator.getAggregate()==0);
		manager.update(position);
		assertTrue(manager.size()==1);
		assertTrue(manager.fetch(id)==position);
		assertTrue(aggregator.getAggregate()==10);
		position = new PositionImpl(id, 10); // amount is irrelevant
		position.setExcluded(true);
		manager.update(position);
		assertTrue(manager.size()==1);
		assertTrue(aggregator.getAggregate()==0);
	}
}
