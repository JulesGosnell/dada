/**
 * 
 */
package com.nomura.cash;

public class PositionImpl extends IdentifiableImpl implements Position {

	protected final int position;

	public PositionImpl(int id, int position) {
		super(id);
		this.position = position;
	}

	public int getPosition() {
		return position;
	}
}