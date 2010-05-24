package org.dada.core;

import java.util.Collection;

import junit.framework.TestCase;

public class AbstractRangeTestCase extends TestCase {

	public static class TestRange extends AbstractRange<Integer> {

		public TestRange(Integer min, Integer max) {
			super(min, max);
		}

		@Override
		public Collection<Integer> getValues() {
			throw new UnsupportedOperationException("NYI");
		}

		@Override
		public Integer random() {
			throw new UnsupportedOperationException("NYI");
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException("NYI");
		}
		
	}
	
	public void test() {
		int min = 0;
		int max = 1;
		Range<Integer> range = new TestRange(min, max);
		assertTrue(range.getMin() == min);
		assertTrue(range.getMax() == max);
	}
	
}
