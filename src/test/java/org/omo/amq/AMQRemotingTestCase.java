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

public class AMQRemotingTestCase extends AbstractRemotingTestCase {

	@Override
	protected ConnectionFactory getConnnectionFactory() {
		return new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
	}

	public void testProxySerialisability() throws Exception {
		TemporaryQueue queue = session.createTemporaryQueue();
		RemotingFactory<Server> factory = new RemotingFactory<Server>(session, Server.class, timeout);
		ExecutorService executorService =  new ThreadPoolExecutor(10, 100, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)); 
		Server server = factory.createServer(new ServerImpl(), queue, executorService);
		Server localClient = factory.createSynchronousClient(queue, true);
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
		Server remoteClient = (Server)ois.readObject();
		logger.info("REUSING PROXY...");
		assertTrue(remoteClient.hashcode(foo) == server.hashcode(foo));
		logger.info("...DONE");
	}

}
