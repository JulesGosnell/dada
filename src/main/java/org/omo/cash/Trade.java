package org.omo.cash;

import org.omo.core.Datum;


public class Trade implements Datum, Comparable<Trade> {

	protected int id;
	protected int version;
	
	public Trade(int id, int version) {
		this.id = id;
		this.version = version;
	}
	
	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getVersion() {
		return version;
	}
	
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version +"]>";
	}

	@Override
	public int compareTo(Trade trade) {
		return id - trade.id;
	}

}
