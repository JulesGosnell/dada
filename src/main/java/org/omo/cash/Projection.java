package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.omo.core.IntegerDatum;

public class Projection extends IntegerDatum {

	private final List<BigDecimal> positions;
	
	public Projection(int id, int version, List<BigDecimal> totals) {
		super(id, version);
		this.positions = new ArrayList<BigDecimal>(totals.size());
		BigDecimal position = BigDecimal.ZERO;
		for (BigDecimal total : totals)
			positions.add(position = position.add(total));
	}
	
	public BigDecimal getPosition(int index) {
		return positions.get(index); 
	}
	
	public String toString() {
		String string = "<" + getClass().getSimpleName() + ":" + id + "[" + version + "]{";
		boolean first = true;
		for (BigDecimal position : positions) {
			if (!first)
				string += ", ";
			else
				first = !first;
			string += position;
		}
		string += "}>";
		return string;
	}
}
