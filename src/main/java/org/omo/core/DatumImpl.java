package org.omo.core;

public class DatumImpl implements Datum {

	protected final int id;
	protected final int version;

	public DatumImpl(int id, int version) {
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
	
	// Object
	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version +"]>";
	}

	// Comparable
	@Override
	public int compareTo(Datum datum) {
		return id - datum.getId();
	}


}
