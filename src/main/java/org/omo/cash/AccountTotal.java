package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.Datum;
import org.omo.core.DatumImpl;

public class AccountTotal extends DatumImpl<Date> {

	private final int account;
	private final BigDecimal amount;
	
	AccountTotal(Date date, int version, int account, BigDecimal amount) {
		super(date, version);
		this.account = account;
		this.amount = amount;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}

	public int getAccount() {
		return account;
	}

	// Comparable
	public int compareTo(Datum<Date> that) {
		return this.id.compareTo(that.getId());
	}
	
	// Object
	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version +"]: " + amount + ">";
	}


	
}
