package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.omo.core.Datum;


public class Trade implements Datum, Comparable<Trade> {

	protected final int id;
	protected final int version;

	private Date valueDate;
	private BigDecimal amount;

	// TODO - lose this ctor
	public Trade(int id, int version) {
		this.id = id;
		this.version = version;
	}
	
	public Trade(int id, int version, Date valueDate, BigDecimal amount) {
		this.id = id;
		this.version = version;
		this.valueDate = valueDate;
		this.amount = amount;
	}	

	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getVersion() {
		return version;
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
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version +"]>";
	}

	// Comparable
	@Override
	public int compareTo(Trade trade) {
		return id - trade.id;
	}

}
