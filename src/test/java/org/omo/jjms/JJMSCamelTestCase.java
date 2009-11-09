package org.omo.jjms;

import javax.jms.ConnectionFactory;

import org.omo.cash.SyncLock;
import org.omo.jms.AbstractCamelTestCase;

import EDU.oswego.cs.dl.util.concurrent.NullSync;

public class JJMSCamelTestCase extends AbstractCamelTestCase {

	@Override
	public ConnectionFactory getConnectionFactory() {
		JJMSConnectionFactory connectionFactory = new JJMSConnectionFactory(executorService, new SyncLock(new NullSync()));
		connectionFactory.start();
		return connectionFactory;
	}

}
