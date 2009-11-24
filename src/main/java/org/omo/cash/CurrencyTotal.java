package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.Datum;
import org.omo.core.DatumImpl;

public class CurrencyTotal extends DatumImpl<Date> {

	private final int currency;
	private final BigDecimal amount;
	
	CurrencyTotal(Date date, int version, int currency, BigDecimal amount) {
		super(date, version);
		this.currency = currency;
		this.amount = amount;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}

	public int getCurrency() {
		return currency;
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
