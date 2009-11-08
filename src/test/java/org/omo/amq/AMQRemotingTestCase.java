package org.omo.amq;

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
import org.omo.jms.AbstractClient;
import org.omo.jms.AbstractRemotingTestCase;
import org.omo.jms.RemotingFactory;
import org.omo.jms.Peer;
import org.omo.jms.PeerImpl;

public class AMQRemotingTestCase extends AbstractRemotingTestCase {

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
