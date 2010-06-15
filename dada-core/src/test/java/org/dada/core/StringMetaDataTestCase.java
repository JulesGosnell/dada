package org.dada.core;

import junit.framework.TestCase;

public class StringMetaDataTestCase extends TestCase {

	public void test() {
		String keyName = "key";
		Metadata<String, String> metadata = new StringMetadata(keyName);
		
		String value = "value";
		assertTrue(metadata.getAttributeValue(value, 0) == value);
	}
}
