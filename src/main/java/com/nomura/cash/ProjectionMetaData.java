package org.omo.cash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.Interval;
import org.omo.core.Metadata;

public class ProjectionMetaData implements Metadata<Integer, Projection> {

	private final List<String> attributeNames;
	
	public ProjectionMetaData(Collection<Interval> intervals) { 
		attributeNames = new ArrayList<String>(2 + intervals.size());
		attributeNames.add("Id");
		attributeNames.add("Version");
		for (Interval interval : intervals)
			attributeNames.add(interval.getStart().toString());
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
