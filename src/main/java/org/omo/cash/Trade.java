package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.DatumImpl;


public class Trade extends DatumImpl {

	private final Date valueDate;
	private final BigDecimal amount;
	private final int account;
	private final int currency;

	public Trade(int id, int version, Date valueDate, BigDecimal amount, int account, int currency) {
		super(id, version);
		this.valueDate = valueDate;
		this.amount = amount;
		this.account = account;
		this.currency = currency;
	}	


	public BigDecimal getAmount() {
		return amount;
	}

	public int getAccount() {
		return account;
	}
	
	public int getCurrency() {
		return currency;
	}
	
	public Date getValueDate() {
		return valueDate;
	}


	// Object
	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version + " valueDate=" + valueDate + ", account=" + account + ", currency=" + currency + "]>";
	}
}
