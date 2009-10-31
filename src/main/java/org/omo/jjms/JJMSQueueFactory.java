package org.omo.jjms;

public class JJMSQueueFactory implements JJMSDestinationFactory {

	@Override
	public JJMSDestination create(String name) {
		return new JJMSQueue(name);
	}

}
