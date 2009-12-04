/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.jjms;

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
		if (messageSelector != null) // TODO: "A message selector is a String, whose syntax is based on a subset of the SQL92 conditional expression syntax."
			throw new UnsupportedOperationException("NYI");
		logger.info("open");
	}

	protected void dispatch(Message message) {
		logger.trace("#{}: receive {}", System.identityHashCode(this), message);
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
			Message message = queue.poll(timeout, TimeUnit.MILLISECONDS);
			logger.trace("receive({}) <- {}", timeout, message);
			return message;
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
			logger.debug("switch to sync mode");
			this.messageListener = defaultMessagelistener; // switch to synchronous mode
		} else {
			logger.debug("switch to async mode");
			this.messageListener = messageListener; // switch to asynchronous mode
			Message message;
			while ((message = receiveNoWait()) != null) // empty synchronous queue onto asynchronous listener
				messageListener.onMessage(message);
		}
	}

}
