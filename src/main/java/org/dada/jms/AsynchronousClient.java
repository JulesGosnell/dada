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
package org.dada.jms;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousClient extends AbstractClient {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, AsyncInvocationListener> correlationIdToListener = new ConcurrentHashMap<String, AsyncInvocationListener>();

	public AsynchronousClient(Session session, Destination destination, Class<?> interfaze, long timeout, boolean trueAsync) throws JMSException {
		super(session, destination, interfaze, timeout, trueAsync);
	}

	public void invoke(Method method, Object[] args, AsyncInvocationListener listener) throws JMSException {
		ObjectMessage message = session.createObjectMessage();
		Integer methodIndex = mapper.getKey(method);
		message.setObject(new Invocation(methodIndex, args));
		String correlationId = "" + count++;
		message.setJMSCorrelationID(correlationId);
		message.setJMSReplyTo(resultsQueue);

		correlationIdToListener.put(correlationId, listener); // TODO: support a timeout after which this listener is removed...
		logger.trace("SENDING: {} -> {}", message, destination);
		producer.send(destination, message);
	}

	@Override
	public void onMessage(Message message) {
		
		if (logger.isInfoEnabled()) { // TODO: yeugh !
			try { 
				logger.info("RECEIVING: {} <- {}", message, message.getJMSDestination());
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	    	
		try {
			String correlationID = message.getJMSCorrelationID();
			AsyncInvocationListener listener = correlationIdToListener.remove(correlationID); // one-shot - parameterize
			if (listener == null) {
				logger.warn("no listener for message: {}", message);
			} else {
				ObjectMessage response = (ObjectMessage) message;
				Results results = (Results) response.getObject();
				Object value = results.getValue();
				if (results.isException()) {
					listener.onError((Exception) value);
				} else {
					listener.onResult(value);
				}
			}
		} catch (JMSException e) {
		        logger.error("problem extracting data from message: {}", message);
		}
	}
}
