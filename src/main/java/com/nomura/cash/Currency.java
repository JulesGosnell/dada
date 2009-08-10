package com.nomura.cash;

import java.util.List;

public interface Currency extends Position {
	
	int getId();
	List<Integer> getAccountIds();

}
