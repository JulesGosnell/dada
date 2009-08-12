package com.nomura.cash;

public class IdentifiableImpl implements Identifiable {

	protected final int id;
	
	public IdentifiableImpl(int id) {
		this.id = id;
	}
	
	public int getId() { 
		return id;
	}
	
}
