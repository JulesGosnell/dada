package org.omo.jjms;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

public class JJMSObjectMessage extends JJMSMessage implements ObjectMessage {

	private Serializable object;
	
	protected JJMSObjectMessage() {
		super();
	}

	protected JJMSObjectMessage(Serializable object) {
		super();
		this.object = object;
	}

	public String toString() {
		return "<" + getClass().getSimpleName() + "#" + System.identityHashCode(this) + ":" + jmsCorrelationId + ": " + object + ">";
	}

	// JMS
	
	@Override
	public Serializable getObject() throws JMSException {
		return object;
	}

	@Override
	public void setObject(Serializable object) throws JMSException {
		this.object = object;
	}

}
