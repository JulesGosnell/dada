package org.omo.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

public class OneWeekRange extends AbstractDateRange {

	protected static final int ONE_DAY = 1000 * 60 * 60 * 24;

	protected final int numDays; // TODO: hook up properly
	
	protected static Date getMin(Calendar calendar) {
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	protected static Date getMax(Calendar calendar) {
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}
	
	public OneWeekRange(int numDays) {
		// 1 week of midnights...
		super(getMin(Calendar.getInstance()), getMax(Calendar.getInstance()));
		this.numDays = numDays;
	}
	
	@Override
	public  boolean contains(Date value) {
		long time = value.getTime();
		return time >= min.getTime() && time <= max.getTime();
	}
	
	@Override
	public Date random() {
		return new Date((long)(Math.random()*(max.getTime()-min.getTime()))+min.getTime());
	}

	@Override
	public Collection<Date> getValues() {
		Collection<Date> values = new ArrayList<Date>();
		for (Date date = min; date.before(max); date = new Date(date.getTime() + ONE_DAY)) {
			values.add(date);
		}
		return values;
	}

	@Override
	public int size() {
		return (int)(max.getTime()-min.getTime()/ONE_DAY);
	}
}
