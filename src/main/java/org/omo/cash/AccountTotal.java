package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.DatumImpl;

public class AccountTotal extends DatumImpl {

	private final Date date;
	private final BigDecimal amount;
	
	AccountTotal(int account, int version, Date date, BigDecimal amount) {
		super(account, version);
		this.date = date;
		this.amount = amount;
	}
	
	public Date getDate() {
		return date;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
}
