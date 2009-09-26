package org.omo.cash;

import org.omo.core.Filter;

public class AccountFilter implements Filter<Trade> {

	private final int account;
	
	public AccountFilter(int account) {
		this.account = account;
	}
	
	@Override
	public boolean apply(Trade trade) {
		return trade.getAccount() == account;
	}

}
