package com.nomura.cash;

public class TradeImpl extends IdentifiableImpl implements Trade {

	final int accountId;
	final int currencyId;
	final int amount;
	
	public TradeImpl(int id, int accountId, int currencyId, int amount) {
		super(id);
		this.accountId = accountId;
		this.currencyId = currencyId;
		this.amount = amount;
	}
	
	@Override
	public int getAccountId() {
		return accountId;
	}

	@Override
	public int getPosition() {
		return amount;
	}

	@Override
	public int getCurrencyId() {
		return currencyId;
	}

	@Override
	public int getId() {
		return id;
	}

}
