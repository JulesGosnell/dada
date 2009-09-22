package org.omo.core;


import java.util.Date;

import junit.framework.TestCase;

public class PerformanceTestCase extends TestCase {

	public void testDates() {
		long now = System.currentTimeMillis();
		for (int i=0; i<10000000;i++)
			new Date();
		System.out.println(System.currentTimeMillis()-now);
	}
}
