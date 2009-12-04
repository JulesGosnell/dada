package org.omo.jjms;

public class JJMSTemporaryQueueFactory extends JJMSQueueFactory {

	@Override
	public JJMSDestination create(String name) {
		return new JJMSTemporaryQueue(name);
	}

}
