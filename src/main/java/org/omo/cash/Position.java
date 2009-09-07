package org.omo.cash;

public interface Position extends Identifiable {

	int getPosition();
	boolean getExcluded();
	void setExcluded(boolean excluded);

}
