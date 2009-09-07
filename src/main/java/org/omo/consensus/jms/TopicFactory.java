package org.omo.consensus.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

public class TopicFactory implements DestinationFactory {

	@Override
	public Destination create(Session session, String name) throws JMSException {
		return session.createTopic(name);
	}

}
