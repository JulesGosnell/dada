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
package org.dada.amq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.jms.TemporaryQueue;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.dada.jms.AbstractClient;
import org.dada.jms.Peer;
import org.dada.jms.PeerImpl;
import org.dada.jms.RemotingAbstractTestCase;
import org.dada.jms.RemotingFactory;

public class AMQRemotingTestCase extends RemotingAbstractTestCase {

	@Override
	protected ConnectionFactory getConnnectionFactory() {
		return new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
	}

	// JJMSTemporaryQueue is not Serialisable, so this test stays here...
	public void testProxyMigration() throws Exception {
		TemporaryQueue queue = session.createTemporaryQueue();
		RemotingFactory<Peer> factory = new RemotingFactory<Peer>(session, Peer.class, timeout);
		ExecutorService executorService =  new ThreadPoolExecutor(10, 100, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));
		Peer server = factory.createServer(new PeerImpl(), queue, executorService);
		Peer localClient = factory.createSynchronousClient(queue, true);
		String foo = "foo";
		assertTrue(localClient.hashcode(foo) == server.hashcode(foo));

		logger.info("MARSHALLING PROXY...");
		ByteArrayOutputStream baos =  new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(localClient);
		logger.info("UNMARSHALLING PROXY...");
		AbstractClient.setCurrentSession(session);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Peer remoteClient = (Peer)ois.readObject();
		logger.info("REUSING PROXY...");
		assertTrue(remoteClient.hashcode(foo) == server.hashcode(foo));
		logger.info("...DONE");
	}

}
