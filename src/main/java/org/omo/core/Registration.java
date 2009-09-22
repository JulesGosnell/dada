package org.omo.core;

import java.io.Serializable;
import java.util.Collection;

public class Registration<K, V> implements Serializable {
	
	private final Metadata<K, V> metadata;
	private final Collection<V> data;
	
	public Registration(Metadata<K, V> metadata, Collection<V> data) {
		this.metadata = metadata;
		this.data = data;
	}
	
	public Metadata<K, V> getMetadata() {
		return metadata;
	}

	public Collection<V> getData() {
		return data;
	}

}
