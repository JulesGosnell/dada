package org.omo.core;

public abstract class DatumImpl<K> implements Datum<K> {

	protected final K id;
	protected final int version;

	public DatumImpl(K id, int version) {
		this.id = id;
		this.version = version;
	}
	
	@Override
	public K getId() {
		return id;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + id + "[" + version +"]>";
	}

}
