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
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.dada.core.ServiceFactory;
import org.dada.core.Target;
import org.dada.jms.JMSServiceFactory.Namer;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class JMSServiceFactoryTestCase extends MockObjectTestCase {

	public void test() throws Exception {
		
		final Session session = (Session)mock(Session.class);
		final ExecutorService executorService = (ExecutorService)mock(ExecutorService.class);
		boolean trueAsync = true;
		long timeout = 1000L;
		final Namer<Target> namer = (Namer<Target>)mock(Namer.class);

		checking(new Expectations(){{
            one(session).createProducer(with(any(Destination.class)));
            will(returnValue(null));            
        }});

		ServiceFactory<Target> serviceFactory = new JMSServiceFactory<Target>(session, Target.class, executorService, trueAsync, timeout, namer);
		final Target target = (Target)mock(Target.class);

		final Queue serverQueue = (Queue)mock(Queue.class, "serverQueue");
		final MessageConsumer serverConsumer = (MessageConsumer)mock(MessageConsumer.class, "serverConsumer");
		final Queue clientQueue = (Queue)mock(Queue.class, "clientQueue");
		final MessageProducer clientProducer = (MessageProducer)mock(MessageProducer.class, "clientProducer");
		final Queue clientServerQueue = (Queue)mock(Queue.class, "clientServerQueue");
		final MessageConsumer clientServerConsumer = (MessageConsumer)mock(MessageConsumer.class, "clientServerConsumer");
		
		// TODO: does this make sense ?
		checking(new Expectations(){{
            String name = "Test";
            one(namer).getName(with(target));
			will(returnValue(name));  
			// set up server side
            one(session).createQueue(name);
			will(returnValue(serverQueue));
            one(session).createConsumer(with(serverQueue));
            will(returnValue(serverConsumer));
            one(serverConsumer).setMessageListener(with(any(MessageListener.class)));
            // set up client side
            one(session).createQueue(with(any(String.class)));
			will(returnValue(clientQueue));
			one(session).createProducer(clientQueue);
			will(returnValue(clientProducer));
            one(session).createQueue(with(any(String.class)));
			will(returnValue(clientServerQueue));
            one(session).createConsumer(with(clientServerQueue));
            will(returnValue(clientServerConsumer));
            one(clientServerConsumer).setMessageListener(with(any(MessageListener.class)));
        }});
        
		serviceFactory.decouple(target);

		// broken client
		checking(new Expectations(){{
            String name = "Test";
            one(namer).getName(with(target));
			will(returnValue(name));  
			// set up server side
            one(session).createQueue(name);
            will(throwException(new UnsupportedOperationException()));
        }});
        
		try {
			serviceFactory.decouple(target);
			fail();
		} catch (RuntimeException e) {
		}

	}
}
