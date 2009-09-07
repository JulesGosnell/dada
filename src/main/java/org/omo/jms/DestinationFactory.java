package org.omo.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

public interface DestinationFactory {

	Destination create(Session session, String name) throws JMSException;
	
}
