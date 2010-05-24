package org.dada.core;

import junit.framework.TestCase;

public class DatumImplTestCase extends TestCase {

	public void test() {
		int id = 1;
		int version = 0;
		Datum<Integer> datum = new DatumImpl<Integer>(id, version) {
			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
		};
		
		assertTrue(datum.getId() == id);
		assertTrue(datum.getVersion() == version);
		assertTrue(datum.toString() != null);
	}
}
