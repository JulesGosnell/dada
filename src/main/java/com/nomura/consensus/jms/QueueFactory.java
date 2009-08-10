package com.nomura.consensus.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

public class QueueFactory implements DestinationFactory {

	@Override
	public Destination create(Session session, String name) throws JMSException {
		return session.createQueue(name);
	}

}
