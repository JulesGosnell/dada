package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;

public class IntegerRange extends AbstractRange<Integer> {

	public IntegerRange(int min, int max) {
		super(min, max);
	}
	
	@Override
	public Integer random() {
		return (int)((Math.random()*(max-min)))+min;
	}

	@Override
	public Collection<Integer> getValues() {
		Collection<Integer> result = new ArrayList<Integer>(max-min);
		for (int i=min; i<max; i++)
			result.add(i);
		return result;
	}

	@Override
	public int size() {
		return max - min;
	}

}
