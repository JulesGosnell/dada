package org.omo.core;

import java.io.Serializable;

public interface Datum<K> extends Serializable, Comparable<Datum<K>> {

	K getId();
	int getVersion();

}
