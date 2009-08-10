package com.nomura.cash;

import java.util.List;

// GETTING CONFUSED...
// Who is responsible for rolling up an account - the Account or the AccountManager ...

public class AccountImpl extends IdentifiableImpl implements Account {

	final int currencyId;
	
	public AccountImpl(int id, int currencyId) {
		super(id);
		this.currencyId = currencyId;
	}
	
	@Override
	public int getCurrencyId() {
		return currencyId;
	}
}
