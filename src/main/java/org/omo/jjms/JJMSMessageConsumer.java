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

	private volatile MessageListener messageListener = defaultMessagelistener; // start in synchronous mode
	
	protected JJMSMessageConsumer(JJMSDestination destination, String messageSelector) {
		this.destination = destination;
		this.messageSelector = messageSelector;
		destination.addMessageConsumer(this);
		if (messageSelector != null) // TODO: implement
			throw new UnsupportedOperationException("NYI");
		logger.info("open");
	}
	
	protected void dispatch(Message message) {
		logger.info("receive {}", message);
		messageListener.onMessage(message);
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
		try {
			return queue.take();
		} catch (InterruptedException e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		try {
			return queue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		return queue.poll();
	}

	@Override
	public void setMessageListener(MessageListener messageListener) throws JMSException {
		if (messageListener == null) {
			this.messageListener = defaultMessagelistener; // switch to synchronous mode
		} else {
			this.messageListener = messageListener; // switch to asynchronous mode
			Message message;
			while ((message = receiveNoWait()) != null) // empty synchronous queue onto asynchronous listener
				messageListener.onMessage(message);
		}
	}

}
