package com.nomura.ltw;

public class IdentifiableImpl implements Identifiable {

	private final int id;
	
	public IdentifiableImpl(int id) {
		this.id = id;
	}
	
	@Override
	public int getId() {
		return id;
	}

}
