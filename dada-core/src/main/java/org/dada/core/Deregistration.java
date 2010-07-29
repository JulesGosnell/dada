package org.dada.core;

import java.io.Serializable;
import java.util.Collection;

public class Deregistration<K, V> implements Serializable {

		private final Collection<V> data;
		
		public Deregistration(Collection<V> data) {
			this.data = data;
		}
		
		public Collection<V> getData() {
			return data;
		}
}
