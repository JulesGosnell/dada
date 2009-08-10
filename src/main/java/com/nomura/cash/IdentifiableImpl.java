package com.nomura.cash;

public class IdentifiableImpl implements Identifiable {

	protected final int id;
	protected boolean excluded = false;
	
	public IdentifiableImpl(int id) {
		this.id = id;
	}
	
	public int getId() { 
		return id;
	}
	
	@Override
	public boolean getExcluded() {
		return excluded;
	}
	
	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}
	
}
