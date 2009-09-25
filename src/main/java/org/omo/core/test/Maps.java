/**
 * 
 */
package org.omo.core.test;

import java.io.Serializable;

import clojure.lang.IPersistentMap;

public class Maps implements Serializable {

	public final IPersistentMap current; // TODO: encapsulate
	public final IPersistentMap historic; // TODO: encapsulate
	
	public Maps(IPersistentMap current, IPersistentMap historic) {
		this.current = current;
		this.historic = historic;
	}

	public IPersistentMap getCurrent() {
		return current;
	}

	public IPersistentMap getHistoric() {
		return historic;
	}

}