package org.omo.core;

import java.util.List;

import javax.jms.Session;

public interface Configuration {

	String getUniversalModelName();
	List<String> getPartitionModelNames();
	
	Session getSession();
	int getTimeout();
	
}
