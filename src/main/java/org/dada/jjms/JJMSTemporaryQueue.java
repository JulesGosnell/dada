package org.omo.jjms;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;

public class JJMSTemporaryQueue extends JJMSQueue implements TemporaryQueue {

	JJMSTemporaryQueue(String name) {
		super(name);
	}
	
	@Override
	public void delete() throws JMSException {
	}
}
