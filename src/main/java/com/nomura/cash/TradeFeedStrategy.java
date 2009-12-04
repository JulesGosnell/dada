/**
 * 
 */
package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.omo.core.Feed;
import org.omo.core.Range;

public final class TradeFeedStrategy implements Feed.Strategy<Integer, Trade> {

	private final Range<Date> dateRange;
	private final Range<Integer> accountRange;
	private final Range<Integer> currencyRange;

	TradeFeedStrategy(Range<Date> dateRange, Range<Integer> accountRange, Range<Integer> currencyRange) {
		this.dateRange = dateRange;
		this.accountRange = accountRange;
		this.currencyRange = currencyRange;
	}

	@Override
	public Collection<Trade> createNewValues(Range<Integer> range) {
		Collection<Trade> values = new ArrayList<Trade>(range.size());
		for (int id : range.getValues()) {
			Date date = dateRange.random();
			BigDecimal amount = new BigDecimal(((int)(100000000*Math.random()))/100);
			int version = 0;
			values.add(new Trade(id, version, date, amount, accountRange.random(), currencyRange.random()));
		}
		return values;
	}

	@Override
	public Trade createNewVersion(Trade original) {
		return new Trade(original.getId(), original.getVersion()+1, original.getValueDate(), new BigDecimal(((int)(100000000*Math.random()))/100), original.getAccount(), original.getCurrency());
	}

	@Override
	public Integer getKey(Trade item) {
		return item.getId();
	}
}