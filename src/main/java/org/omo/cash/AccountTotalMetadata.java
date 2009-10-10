package org.omo.cash;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.omo.core.Metadata;

public class AccountTotalMetadata implements Metadata<Date, AccountTotal> {

	@Override
	public List<String> getAttributeNames() {
		List<String> list = new ArrayList<String>(3);
		list.add("Date");
		list.add("Version");
		list.add("Amount");
		return list;
	}

	@Override
	public Object getAttributeValue(AccountTotal value, int index) {
		switch (index) {
		case 0:
			return value.getDate();
		case 1:
			return value.getVersion();
		case 2:
			return value.getAmount();
		}
		throw new IllegalArgumentException("index out of bounds:" + index);
	}

	@Override
	public Date getKey(AccountTotal value) {
		return value.getDate();
	}
}
