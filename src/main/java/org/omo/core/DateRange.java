package org.omo.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

public class DateRange extends AbstractRange<Date> {

	protected static Calendar calendar = Calendar.getInstance();

	protected static Date getMin() {
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	protected static Date getMax() {
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}
	
	public DateRange() {
		// 1 week of midnights...
		super(getMin(), getMax());
	}
	
	@Override
	public  boolean contains(Date value) {
		return min.before(value) && max.after(value);
	}
	
	@Override
	public Date random() {
		return new Date((long)(Math.random()*(max.getTime()-min.getTime()))+min.getTime());
	}

	@Override
	public Collection<Date> getValues() {
		Collection<Date> values = new ArrayList<Date>();
		for (Date date = min; date.before(max); date = new Date(date.getTime() + (1000 * 60 * 60 * 24))) {
			values.add(date);
		}
		return values;
	}

	@Override
	public int size() {
		return (int)(max.getTime()-min.getTime()/24/60/60/1000);
	}
}
