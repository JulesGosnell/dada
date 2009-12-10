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
package org.dada.consensus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.dada.jms.AsyncInvocationListener;
import org.dada.jms.AsynchronousClient;
import org.dada.jms.RemotingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConsensusTestCase extends TestCase {

	private static final Logger LOG = LoggerFactory.getLogger(ConsensusTestCase.class);
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Destination destination;
	private RemotingFactory<Paxos> remotingFactory;

	public static interface Paxos {
		int foo();
	};

	public static class PaxosImpl implements Paxos {
		@Override
		public int foo() { return 1;}
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		destination = session.createTopic(Paxos.class.getCanonicalName());
		int timeout = 5000;
		remotingFactory = new RemotingFactory<Paxos>(session, Paxos.class, timeout);
	}

	@Override
	protected void tearDown() throws Exception {
		session.close();
		session = null;
		connection.stop();
		connection.close();
		connection = null;
		connectionFactory = null;
		super.tearDown();
	}

	public void testTopic() throws Exception {
		ExecutorService executorService =  new ThreadPoolExecutor(10, 100, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));
		Paxos server1 = remotingFactory.createServer(new PaxosImpl(), destination, executorService);
		Paxos server2 = remotingFactory.createServer(new PaxosImpl(), destination, executorService);
		Paxos server3 = remotingFactory.createServer(new PaxosImpl(), destination, executorService);
		Paxos server4 = remotingFactory.createServer(new PaxosImpl(), destination, executorService);
		AsynchronousClient client = remotingFactory.createAsynchronousClient(destination, true);

		client.invoke(Paxos.class.getMethod("foo", (Class<?>[])null), null, new AsyncInvocationListener(){

			@Override
			public void onError(Exception exception) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onResult(Object value) {
				LOG.info("foo = " + value);
			}});

		Thread.sleep(5000);
	}

}
