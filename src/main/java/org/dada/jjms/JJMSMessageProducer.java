package org.omo.jjms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JJMSMessageProducer implements MessageProducer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final JJMSSession session;
	private Destination destination;
	private int deliveryMode;
	private int priority;
	private long timeToLive;
	
	protected JJMSMessageProducer(JJMSSession session, Destination destination) {
		this.session = session;
		this.destination = destination;
		logger.info("open");
	}
	
	@Override
	public void close() throws JMSException {
		logger.info("close");
	}

	@Override
	public int getDeliveryMode() throws JMSException {
		return deliveryMode;
	}

	@Override
	public Destination getDestination() throws JMSException {
		return destination;
	}

	@Override
	public boolean getDisableMessageID() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public int getPriority() throws JMSException {
		return priority;
	}

	@Override
	public long getTimeToLive() throws JMSException {
		return timeToLive;
	}

	@Override
	public void send(Message message) throws JMSException {
		send(destination, message);
	}

	@Override
	public void send(Destination destination, Message message) throws JMSException {
		logger.trace("#" + System.identityHashCode(this) + ": send {} -> {}", message, destination);
		session.send((JJMSMessage)message, (JJMSDestination)destination);
	}

	@Override
	public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		message.setJMSDeliveryMode(deliveryMode);
		message.setJMSPriority(priority);
		long timestamp = System.currentTimeMillis();
		message.setJMSTimestamp(timestamp);
		message.setJMSExpiration(timestamp + timeToLive); // TODO: is this correct ?
		send(message);
	}

	@Override
	public void send(Destination arg0, Message arg1, int arg2, int arg3,
			long arg4) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException {
		this.deliveryMode = deliveryMode;  
	}

	@Override
	public void setDisableMessageID(boolean arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setDisableMessageTimestamp(boolean arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setPriority(int priority) throws JMSException {
		this.priority = priority;
	}

	@Override
	public void setTimeToLive(long timeToLive) throws JMSException {
		this.timeToLive = timeToLive;
	}

}
