package org.omo.old;

import java.util.Date;

public class DatePartitioningStrategy implements PartitioningStrategy<Date> {

	protected final Date start;
	protected final long granularity;

	public DatePartitioningStrategy(Date start, long granularity) {
		this.start = start;
		this.granularity = granularity;
	}
	
	@Override
	public int getPartition(Date date) {
		long delta = date.getTime()-start.getTime();
		long partition = delta/granularity;
		return (int)partition;
	}
	
}
