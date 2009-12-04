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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class JJMSConnectionFactory implements ConnectionFactory {

	private class Job implements Runnable {

		private final JJMSMessage message;
		private final JJMSDestination destination;

		private Job(JJMSMessage message, JJMSDestination destination) {
			this.message = message;
			this.destination = destination;
		}

		@Override
		public void run() {
			try {
				destination.dispatch(message);
			} finally {
				lock.unlock(); // decrement lock count - this message is now being processed...
			}
		}

	}

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();
	private final ExecutorService threadPool;
	private final Lock lock;

	private volatile IPersistentMap nameToDestination= PersistentTreeMap.EMPTY;
	private volatile boolean running;
	private volatile Thread thread;

	public JJMSConnectionFactory(ExecutorService threadPool, Lock lock) {
		logger.debug("create");
		this.threadPool = threadPool;
		this.lock = lock;
	}

	protected JJMSDestination ensureDestination(String name, JJMSDestinationFactory destinationFactory) {
		JJMSDestination destination = (JJMSDestination) nameToDestination.valAt(name);
		if (destination == null) {
			nameToDestination = nameToDestination.assoc(name, destination = destinationFactory.create(name));
		}
		return destination;
	}

	public void send(JJMSMessage message, JJMSDestination destination) throws JMSException {
		message.setJMSDestination(destination);
		lock.lock(); // increment lock count - this message is now in-flight...
		jobs.add(new Job(message, destination));
	}


	public void start() {
		logger.debug("start");
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				while (running) {
					try {
						threadPool.execute(jobs.take());
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}});
		thread.start();
	}

	public void stop() {
		running = false;
		logger.debug("stop");
	}

	// JMS

	@Override
	public Connection createConnection() throws JMSException {
		return new JJMSConnection(this);
	}

	@Override
	public Connection createConnection(String arg0, String arg1) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

}
