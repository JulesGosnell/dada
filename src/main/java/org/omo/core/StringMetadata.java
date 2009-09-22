/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.List;


public class StringMetadata implements Metadata<String, String> {

	private final List<String> attributeNames = new ArrayList<String>();

	public StringMetadata(String keyName) {
		attributeNames.add(keyName);
	}
	
	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	@Override
	public Object getAttributeValue(String value, int index) {
		return value;
	}

	@Override
	public String getKey(String value) {
		return value;
	}
}