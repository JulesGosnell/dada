package org.omo.core;

import java.io.Serializable;

public interface Datum extends Serializable, Comparable<Datum> {

	int getId();
	int getVersion();

}
