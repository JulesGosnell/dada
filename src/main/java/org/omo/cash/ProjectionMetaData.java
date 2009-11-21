package org.omo.cash;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.omo.core.OneWeekRange;
import org.omo.core.Metadata;

public class ProjectionMetaData implements Metadata<Integer, Projection> {

	private final List<String> attributeNames;
	
	public ProjectionMetaData(OneWeekRange range) { 
		attributeNames = new ArrayList<String>(2 + range.getValues().size());
		attributeNames.add("Id");
		attributeNames.add("Version");
		for (Date date : range.getValues())
			attributeNames.add(date.toString());
	}
	
	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	@Override
	public Object getAttributeValue(Projection value, int index) {
		switch (index) {
		case 0:
			return value.getId();
		case 1:
			return value.getVersion();
		default:
			return value.getPosition(index - 2);
		}
	}

	@Override
	public Integer getKey(Projection value) {
		return value.getId();
	}

}
