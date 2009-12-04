package org.omo.jjms;

import javax.jms.JMSException;
import javax.jms.Topic;

public class JJMSTopic extends JJMSDestination implements Topic {

	protected JJMSTopic(String name) {
		super(name);
	}

	@Override
	protected void dispatch(JJMSMessage message) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	// JMS
	
	@Override
	public String getTopicName() throws JMSException {
		return getName();
	}

}
