package org.omo.core;

public class IntegerDatum extends DatumImpl<Integer> {

	public IntegerDatum(int id, int version) {
		super(id, version);
	}

	@Override
	public int compareTo(Datum<Integer> datum) {
		return id - datum.getId();
	}


}
