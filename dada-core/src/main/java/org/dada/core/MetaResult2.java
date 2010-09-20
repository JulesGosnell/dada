package org.dada.core;

import java.io.Serializable;
import java.util.Collection;

public class MetaResult2 implements Serializable {

	private final Collection<Metadata<?, ?>> metadataList;
	private final Collection<Object> keyList;

	public MetaResult2(Collection<Metadata<?, ?>> metadataList, Collection<Object> keyList) {
		super();
		this.metadataList = metadataList;
		this.keyList= keyList;
	}

	public Collection<Metadata<?, ?>> getMetadataList() {
		return metadataList;
	}

	public Collection<Object> getKeyList() {
		return keyList;
	}

}
