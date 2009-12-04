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

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JJMSConnection implements Connection {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final JJMSConnectionFactory connectionFactory;

	private volatile ExceptionListener exceptionListener;

	protected JJMSConnection(JJMSConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		logger.info("open");
	}

	protected JJMSDestination ensureDestination(String name, JJMSDestinationFactory destinationFactory) {
		return connectionFactory.ensureDestination(name, destinationFactory);
	}

	public void send(JJMSMessage message, JJMSDestination destination) throws JMSException {
		connectionFactory.send(message, destination);
	}

	// JMS...

	@Override
	public void close() throws JMSException {
		logger.info("close");
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Destination arg0, String arg1, ServerSessionPool arg2, int arg3) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return new JJMSSession(this, transacted, acknowledgeMode);
	}

	@Override
	public String getClientID() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return exceptionListener;
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setClientID(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException {
		exceptionListener = listener;
	}

	@Override
	public void start() throws JMSException {
	}

	@Override
	public void stop() throws JMSException {
	}

}
