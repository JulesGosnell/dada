/**
 * 
 */
package org.omo.core;

import java.util.Collection;
import java.util.Date;

public class DateRange extends AbstractRange<Date> {

	public DateRange(Date min, Date max) {
		super(min, max);
	}
	
	@Override
	public String toString() {
		return "" + min + "/" + max;
	}

	@Override
	public Collection<Date> getValues() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Date random() {
		return new Date(min.getTime() + (long)(Math.random() * (max.getTime() - min.getTime())));
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	
}