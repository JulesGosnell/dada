package org.dada.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import clojure.lang.Indexed;

public class Data<V> implements Serializable, Indexed {

	private final Collection<V> extant;
	private final Collection<V> extinct;
		
		public Data(Collection<V> extant, Collection<V> extinct) {
			this.extant = (Collection<V>) (extant == null  ? Collections.emptyList() : extant);
			this.extinct = (Collection<V>) (extinct == null  ? Collections.emptyList() : extinct);
		}
		
		public Collection<V> getExtant() {
			return extant;
		}

		public Collection<V> getExtinct() {
			return extinct;
		}

		@Override
		public int count() {
			return 2;
		}

		@Override
		public Object nth(int i) {
			switch (i) {
			case 0:
				return extant;
			case 1:
				return extinct;
			default:
				throw new IndexOutOfBoundsException("invalid index: " + i);
			}
		}

		@Override
		public Object nth(int i, Object notFound) {
			switch (i) {
			case 0:
				return extant;
			case 1:
				return extinct;
			default:
				return notFound;
			}
		}

		// TODO - should be in utils class

		private static boolean safeEquals(Object lhs, Object rhs) {
		    return (lhs == rhs || (lhs != null && rhs != null && lhs.equals(rhs)));
		}

		@Override
		public boolean equals(Object that) {
		    return (that != null &&
			    that instanceof Data &&
			    safeEquals(extant, ((Data)that).extant) &&
			    safeEquals(extinct, ((Data)that).extinct));
		}
}
