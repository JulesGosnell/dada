package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.DatumImpl;


public class Trade extends DatumImpl {

	private Date valueDate;
	private BigDecimal amount;

	// TODO - lose this ctor
	public Trade(int id, int version) {
		super(id, version);
	}
	
	public Trade(int id, int version, Date valueDate, BigDecimal amount) {
		super(id, version);
		this.valueDate = valueDate;
		this.amount = amount;
	}	


	public Date getValueDate() {
		return valueDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	// Object
	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version + "," + valueDate + "]>";
	}
}
