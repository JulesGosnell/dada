package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.DatumImpl;


public class Trade extends DatumImpl {

	private final Date valueDate;
	private final BigDecimal amount;
	private final int account;

	public Trade(int id, int version, Date valueDate, BigDecimal amount, int account) {
		super(id, version);
		this.valueDate = valueDate;
		this.amount = amount;
		this.account = account;
	}	


	public Date getValueDate() {
		return valueDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public int getAccount() {
		return account;
	}
	
	// Object
	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version + " valueDate=" + valueDate + ", account=" + account +"]>";
	}
}
