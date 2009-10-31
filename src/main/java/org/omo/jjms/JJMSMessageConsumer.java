package org.omo.jjms;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.UncategorizedJmsException;

public class JJMSMessageConsumer implements MessageConsumer {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final JJMSSession session;
	private final JJMSDestination destination;
	//private final BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>(100); 

	private volatile MessageListener messageListener;
	
	protected JJMSMessageConsumer(JJMSSession session, JJMSDestination destination) {
		this.session = session;
		this.destination = destination;
		logger.info("open");
	}
	
	protected void dispatch(Message message) {
		logger.info("receive {}", message);
		MessageListener snapshot = messageListener;
		if (snapshot != null)
			snapshot.onMessage(message);
	}
	
	@Override
	public void close() throws JMSException {
		logger.info("close");
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getMessageSelector() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Message receive() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	private Message receiveHack;

	@Override
	public Message receive(long timeout) throws JMSException {
		final CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
		final MessageListener oldMessageListener = messageListener;
		setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				messageListener = oldMessageListener;
				receiveHack = message;
				latch.countDown();
			}
		});
		try {
			if (latch.await(timeout, TimeUnit.MILLISECONDS))
				return receiveHack;
			else {
				logger.warn("receive() timed out");
				return null;
			}
		} catch (InterruptedException e) {
			throw new UncategorizedJmsException(e);
		}
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setMessageListener(MessageListener newMessageListener) throws JMSException {
		MessageListener oldMessageListener = this.messageListener;
		this.messageListener = newMessageListener;
		if (oldMessageListener == null) {
			if (newMessageListener != null) {
				destination.addMessageConsumer(this);
			}
		} else {
			if (newMessageListener == null) {
				destination.removeMessageConsumer(this);
			}
		}
	}

}
