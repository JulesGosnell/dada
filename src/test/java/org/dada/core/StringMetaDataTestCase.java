package org.dada.core;

import junit.framework.TestCase;

public class StringMetaDataTestCase extends TestCase {

	public void test() {
		String keyName = "key";
		Metadata<String, String> metadata = new StringMetadata(keyName);
		
		assertTrue(metadata.getAttributeNames().contains(keyName));
		assertTrue(metadata.getAttributeNames().size() == 1);
		String value = "value";
		assertTrue(metadata.getAttributeValue(value, 0) == value);
		assertTrue(metadata.getKey(value) == value);
	}
}
