package com.nomura.cash2;


public class Trade implements Datum {

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

}
