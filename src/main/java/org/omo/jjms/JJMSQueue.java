package org.omo.jjms;

import javax.jms.JMSException;
import javax.jms.Queue;

public class JJMSQueue extends JJMSDestination implements Queue {

	protected JJMSQueue(String name) {
		super(name);
	}
	
	// JMS
	
	@Override
	public String getQueueName() throws JMSException {
		return getName();
	}


}
