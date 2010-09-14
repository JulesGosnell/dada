package org.dada.swt;

public class Mutable<T> {
	
	private T datum;
	
	public Mutable(T datum) {
		this.datum = datum;
	}
	
	public synchronized T getDatum() {
		return datum;
	}

	public synchronized void setDatum(T datum) {
		this.datum = datum;
	}

	@Override
	public String toString() {
		return "<Mutable:" + datum.toString() + ">";
	}
	
}
