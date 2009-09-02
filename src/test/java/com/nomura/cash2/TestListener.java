package com.nomura.cash2;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestListener<T> implements Listener<T>, Serializable {

	private final Log log = LogFactory.getLog(getClass());
	
	@Override
	public void update(List<T> updates) {
		log.info("TEST LISTENER: UPDATE("+updates+")");
	}

	@Override
	public void update(T update) {
		log.info("TEST LISTENER: UPDATE("+update+")");
	}

}
