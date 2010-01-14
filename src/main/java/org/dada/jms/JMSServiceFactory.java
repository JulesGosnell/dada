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

import java.util.concurrent.ExecutorService;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.dada.core.Getter;
import org.dada.core.ServiceFactory;

// TODO - This abstraction wraps an earlier one - we should collapse and simplify the two...
// TODO - share code between JMS and Asynchronous Transports (trueAsync, Invocation)

/**
 * A ServiceFactory that will transparently pass invocations forwards/backwards over a JMS provider.
 * 
 * @author jules
 *
 * @param <T>
 */
public class JMSServiceFactory<T> implements ServiceFactory<T> {

	public static interface DestinationFactory {
		Destination createDestination(Session session, String endPoint);
	}
	
	private final Session session;
	private final RemotingFactory<T> factory;
	private final boolean trueAsync;
	private final ExecutorService executorService;
	private final Getter<String, T> nameGetter;
	private final DestinationFactory destinationFactory;
	
	public JMSServiceFactory(Session session, Class<?> interfaze, ExecutorService executorService, boolean trueAsync, long timeout, Getter<String, T> nameGetter, DestinationFactory destinationFactory) throws JMSException {
		this.session = session;
		this.factory = new RemotingFactory<T>(session, interfaze, timeout); // TODO - support multiple interfaces
		this.executorService = executorService;
		this.trueAsync = trueAsync;
		this.nameGetter = nameGetter;
		this.destinationFactory = destinationFactory;
	}
	
	@Override
	public T decouple(T target) {	
		String name = nameGetter.get(target);
		try {
			server(target, name);
			return client(name);
		} catch (Exception e) {
			// soften exception..
			throw new RuntimeException("problem decoupling client/server over JMS", e);
		}
	}

	@Override
	public T client(String endPoint) throws Exception {
		return factory.createSynchronousClient(endPoint, trueAsync);
	}
	
	@Override
	public void server(T model, String endPoint) throws Exception {
		factory.createServer(model, destinationFactory.createDestination(session, endPoint), executorService);
	}

}
