package org.dada.core;

import java.io.Serializable;

import clojure.lang.Indexed;

public class MetaAttribute<K, V> implements Serializable, Indexed {
	
	private final K key;
	private final Class<V> type;
	private final Getter<K, V> getter;
	private final boolean mutable;
	
	public MetaAttribute(K key, Class<V> type, Getter<K, V> getter, boolean mutable) {
		this.key = key;
		this.mutable = mutable;
		this.type = type;
		this.getter = getter;
	}

	public K getKey() {
		return key;
	}

	public boolean isMutable() {
		return mutable;
	}

	public Class<V> getType() {
		return type;
	}

	public Getter<K, V> getGetter() {
		return getter;
	}

	// Indexed - so we can use destructuring from Clojure...
	
	@Override
	public int count() {
		return 4;
	}

	@Override
	public Object nth(int i) {
		switch (i) {
		case 0: return key;
		case 1: return type;
		case 2: return getter;
		case 3: return mutable;
		default: throw new IndexOutOfBoundsException();
		}
	}

}
