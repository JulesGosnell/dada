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

import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;

import org.dada.core.SyncLock;
import org.dada.jjms.JJMSConnectionFactory;
import org.dada.jms.AbstractRemotingTestCase;
import org.dada.jms.Unserialisable;

import EDU.oswego.cs.dl.util.concurrent.NullSync;

public class JJMSRemotingTestCase extends AbstractRemotingTestCase {

	@Override
	protected ConnectionFactory getConnnectionFactory() {
		JJMSConnectionFactory connectionFactory = new JJMSConnectionFactory(Executors.newFixedThreadPool(2), new SyncLock(new NullSync()));
		connectionFactory.start();
		return connectionFactory;
	}


	public void testSendAsyncReceiveUnserialisable() throws Exception {
		testSendAsyncReceive(new Unserialisable());
	}
}