package org.omo.cash;

import org.omo.core.Filter;

public class CurrencyFilter implements Filter<Trade> {

	private final int currency;
	
	public CurrencyFilter(int currency) {
		this.currency = currency;
	}
	
	@Override
	public boolean apply(Trade trade) {
		return trade.getCurrency() == currency;
	}

}
