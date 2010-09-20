package org.dada.core;

import java.io.Serializable;
import java.util.Collection;

public class Result2 implements Serializable {

	private final Collection<Model<?, ?>> modelList;
	private final Collection<Collection<Object>> pairList;
	
	public Result2(Collection<Model<?, ?>> modelList, Collection<Collection<Object>> pairList) {
		super();
		this.modelList = modelList;
		this.pairList = pairList;
	}

	public Collection<Model<?, ?>> getModelList() {
		return modelList;
	}

	public Collection<Collection<Object>> getPairList() {
		return pairList;
	}

}
