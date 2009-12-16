package org.dada.core;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import junit.framework.TestCase;

public class IntrospectiveMetadataTestCase extends TestCase {

	public void test() throws SecurityException, NoSuchMethodException {
		int key = 1;
		int version = 0;
		final RuntimeException exception = new UnsupportedOperationException("NYI");  
		DatumImpl<Integer> datum = new DatumImpl<Integer>(key, version) {
			@Override
			public int compareTo(Datum<Integer> o) {
				throw new UnsupportedOperationException("NYI");
			}
			
			// doesn't begin with "get"
			public void dummyMethod() {
			}
			// does begin with "get" but is not a getter 
			public void getafix(int i) {
			}
			// is a getter, but throws an exception
			public int getStuffed() {
				throw exception;
			}
		};
		
		IntrospectiveMetadata<Integer, Datum<Integer>> metadata = new IntrospectiveMetadata<Integer, Datum<Integer>>(datum.getClass(), "Id");
		List<String> attributeNames = metadata.getAttributeNames();
		assertTrue(attributeNames.contains("Id"));
		assertTrue(attributeNames.contains("Version"));
		assertTrue(attributeNames.contains("Stuffed"));
		
		assertTrue(metadata.getKey(datum).equals(key));
		
		try {
			metadata.getAttributeValue(datum, attributeNames.indexOf("Stuffed"));
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(((InvocationTargetException)e.getCause()).getTargetException() == exception);
		}
	}
}
