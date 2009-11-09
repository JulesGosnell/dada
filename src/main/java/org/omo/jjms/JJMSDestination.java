package org.omo.jjms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.jms.Destination;

public abstract class JJMSDestination implements Destination {
	
	protected final String name;
	protected volatile Collection<JJMSMessageConsumer> messageConsumers = Collections.emptyList();
	
	protected JJMSDestination(String name) {
		this.name = name;
	}
	
	protected void addMessageConsumer(JJMSMessageConsumer messageConsumer) {
		synchronized (messageConsumers) {
			Collection<JJMSMessageConsumer> newMessageConsumers = new ArrayList<JJMSMessageConsumer>(messageConsumers);
			newMessageConsumers.add(messageConsumer);
			messageConsumers = newMessageConsumers;
		}
	}
	
	protected void removeMessageConsumer(JJMSMessageConsumer messageConsumer) {
		synchronized (messageConsumers) {
			Collection<JJMSMessageConsumer> newMessageConsumers = new ArrayList<JJMSMessageConsumer>(messageConsumers);
			newMessageConsumers.remove(messageConsumer);
			messageConsumers = newMessageConsumers;
		}
	}
	
	protected void dispatch(JJMSMessage message) {
		// TODO: push down and differentiate queue/topic
		for (JJMSMessageConsumer messageConsumer : messageConsumers) {
			messageConsumer.dispatch(message);
		}
	}

	protected String getName() {
		return getName();
	}

	public String toString() {
		return "<" + getClass().getSimpleName() + ":" + System.identityHashCode(this) + ":" + name + ">";
	}

}
