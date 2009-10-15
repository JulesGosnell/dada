package org.omo.cash;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.spi.LoggerFactory;
import org.omo.cash.Projection;


public class ProjectionTestCase extends TestCase {

	public void testProjection() {
		List<BigDecimal> totals = new ArrayList<BigDecimal>();
		totals.add(new BigDecimal(0));
		totals.add(new BigDecimal(1));
		totals.add(new BigDecimal(2));
		totals.add(new BigDecimal(3));
		totals.add(new BigDecimal(4));
		totals.add(new BigDecimal(5));
		totals.add(new BigDecimal(-5));
		totals.add(new BigDecimal(-4));
		totals.add(new BigDecimal(-3));
		totals.add(new BigDecimal(-2));
		totals.add(new BigDecimal(-1));
		totals.add(new BigDecimal(-0));

		Projection projection = new Projection(50, 100, totals);
		LogFactory.getLog(getClass()).info(projection);

		assertTrue(projection.getId() == 50);
		assertTrue(projection.getVersion() == 100);
		assertTrue(projection.getPosition(0).equals(new BigDecimal(0)));
		assertTrue(projection.getPosition(1).equals(new BigDecimal(1)));
		assertTrue(projection.getPosition(2).equals(new BigDecimal(3)));
		assertTrue(projection.getPosition(3).equals(new BigDecimal(6)));
		assertTrue(projection.getPosition(4).equals(new BigDecimal(10)));
		assertTrue(projection.getPosition(5).equals(new BigDecimal(15)));
		assertTrue(projection.getPosition(6).equals(new BigDecimal(10)));
		assertTrue(projection.getPosition(7).equals(new BigDecimal(6)));
		assertTrue(projection.getPosition(8).equals(new BigDecimal(3)));
		assertTrue(projection.getPosition(9).equals(new BigDecimal(1)));
		assertTrue(projection.getPosition(10).equals(new BigDecimal(0)));
	}
}
