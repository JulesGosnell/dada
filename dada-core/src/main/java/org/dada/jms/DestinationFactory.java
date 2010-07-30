package org.dada.jms;

import javax.jms.Destination;
import javax.jms.Session;

public interface DestinationFactory {
	Destination createDestination(Session session, String endPoint);
}