package org.omo.jjms;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JJMSMessageConsumer implements MessageConsumer {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
	private final MessageListener defaultMessagelistener = new MessageListener() {@Override public void onMessage(Message message) {queue.add(message);}}; 
	private final JJMSDestination destination;
	private final String messageSelector;

	private volatile MessageListener messageListener = defaultMessagelistener;
	
	protected JJMSMessageConsumer(JJMSDestination destination, String messageSelector) {
		// default behaviour - push messages onto queue for receive() - replaced
		// if user calls setMessageListener()...
		this.destination = destination;
		this.messageSelector = messageSelector;
		destination.addMessageConsumer(this);
		if (messageSelector != null) // TODO: implement
			throw new UnsupportedOperationException("NYI");
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
		destination.removeMessageConsumer(this);
		logger.info("close");
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return messageListener;
	}

	@Override
	public String getMessageSelector() throws JMSException {
		return messageSelector;
	}

	@Override
	public Message receive() throws JMSException {
		assert messageListener != null : "messageListener is unset";
		try {
			return queue.take();
		} catch (InterruptedException e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		assert messageListener != null  : "messageListener is unset";
		try {
			return queue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		assert messageListener != null : "messageListener is unset";
		return queue.poll();
	}

	@Override
	public void setMessageListener(MessageListener newMessageListener) throws JMSException {
		this.messageListener = newMessageListener;
	}

}
