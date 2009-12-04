package org.omo.amq;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.omo.jms.AbstractCamelTestCase;

public class AMQCamelTestCase extends AbstractCamelTestCase {

	@Override
	public ConnectionFactory getConnectionFactory() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useShutdownHook=false");
		connectionFactory.setOptimizedMessageDispatch(true); // don't know - possibly defaut anyway
		connectionFactory.setObjectMessageSerializationDefered(true); // do not serialise on send - only use object once 
		connectionFactory.setCopyMessageOnSend(false); // only use a message once
		return connectionFactory;
	}

}
