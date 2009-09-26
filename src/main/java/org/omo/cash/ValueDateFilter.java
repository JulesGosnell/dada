package org.omo.cash;

import java.util.Date;

import org.omo.core.Filter;

public class ValueDateFilter implements Filter<Trade> {

	private final Date start;
	private final Date end;
	
	public ValueDateFilter(Date start, Date end) {
		this.start = start;
		this.end= end;
	}
	
	@Override
	public boolean apply(Trade trade) {
		Date valueDate = trade.getValueDate();
		return valueDate.after(start) && valueDate.before(end);
	}

}
