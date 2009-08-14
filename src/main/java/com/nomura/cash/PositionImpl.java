/**
 * 
 */
package com.nomura.cash;

public class PositionImpl extends IdentifiableImpl implements Position {

	protected final int position;
	protected boolean excluded = false;

	public PositionImpl(int id, int position) {
		super(id);
		this.position = position;
	}

	@Override
	public int getPosition() {
		return position;
	}
	
	@Override
	public boolean getExcluded() {
		return excluded;
	}
	
	@Override
	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}
	
}