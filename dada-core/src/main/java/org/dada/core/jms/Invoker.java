package org.dada.core.jms;

import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

public interface Invoker<T> {

	void invoke(T target, Session session, Message message, MessageProducer producer);
	
}
