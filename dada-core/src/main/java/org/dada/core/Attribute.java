package org.dada.core;

import java.io.Serializable;

import clojure.lang.Indexed;

public class Attribute<K, V> implements Serializable, Indexed {
	
	private final K key;
	private final Class<V> type;
	private final boolean mutable;
	private final Getter<K, V> getter;
	
	public Attribute(K key, Class<V> type, boolean mutable, Getter<K, V> getter) {
		this.key = key;
		this.type = type;
		this.mutable = mutable;
		this.getter = getter;
	}

	public K getKey() {
		return key;
	}

	public Class<V> getType() {
		return type;
	}

	public boolean getMutable() {
		return mutable;
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
		case 2: return mutable;
		case 3: return getter;
		default: throw new IndexOutOfBoundsException();
		}
	}

	//@Override
	public Object nth(int i, Object notFound) {
		switch (i) {
		case 0: return key;
		case 1: return type;
		case 2: return mutable;
		case 3: return getter;
		default: return notFound;
		}
	}

	@Override
	public String toString() {
		return "<Attribute: " + key + " " + type + " " + mutable + " " + getter + ">";
	}
	
}
