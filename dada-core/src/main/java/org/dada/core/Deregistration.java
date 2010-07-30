package org.dada.core;

import java.io.Serializable;
import java.util.Collection;

public class Deregistration<V> implements Serializable {

	private final Collection<V> extant;
	private final Collection<V> extinct;
		
		public Deregistration(Collection<V> extant, Collection<V> extinct) {
			this.extant = extant;
			this.extinct = extinct;
		}
		
		public Collection<V> getExtant() {
			return extant;
		}

		public Collection<V> getExtinct() {
			return extinct;
		}
}
