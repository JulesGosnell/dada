package org.omo.old;

public class IdentifiableImpl implements Identifiable {

	protected final int id;
	
	public IdentifiableImpl(int id) {
		this.id = id;
	}
	
	public int getId() { 
		return id;
	}
	
}
