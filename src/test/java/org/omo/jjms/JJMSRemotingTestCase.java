package org.omo.jjms;

import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;

import org.omo.cash.SyncLock;
import org.omo.jms.AbstractRemotingTestCase;
import org.omo.jms.Unserialisable;

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
