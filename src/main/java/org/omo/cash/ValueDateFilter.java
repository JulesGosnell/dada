package org.omo.cash;

import java.util.Date;

import org.omo.core.Filter;

public class ValueDateFilter implements Filter<Trade> {

	private final Date date;
	
	public ValueDateFilter(Date date) {
		this.date = date;
	}
	
	@Override
	public boolean apply(Trade trade) {
		Date valueDate = trade.getValueDate();
		long difference = valueDate.getTime() - date.getTime();
		return difference >= 0 && difference < (1000 * 60 * 60 * 24); // 00:00:00.000 - 23.59.59.999
	}

}
