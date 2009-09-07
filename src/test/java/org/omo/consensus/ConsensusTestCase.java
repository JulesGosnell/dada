package org.omo.consensus;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.jms.AsyncInvocationListener;
import org.omo.jms.AsynchronousClient;
import org.omo.jms.DestinationFactory;
import org.omo.jms.RemotingFactory;
import org.omo.jms.TopicFactory;


public class ConsensusTestCase extends TestCase {

	private static final Log LOG = LogFactory.getLog(ConsensusTestCase.class);
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private RemotingFactory<Paxos> remotingFactory;
	
	public static interface Paxos {
		int foo();
	};
	
	public static class PaxosImpl implements Paxos {
		public int foo() { return 1;}
	};

	protected void setUp() throws Exception {
		super.setUp();
		connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		DestinationFactory destinationFactory = new TopicFactory();
		int timeout = 5000;
		remotingFactory = new RemotingFactory<Paxos>(session, Paxos.class, destinationFactory, timeout);
	}

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
		Paxos server1 = remotingFactory.createServer(new PaxosImpl());
		Paxos server2 = remotingFactory.createServer(new PaxosImpl());
		Paxos server3 = remotingFactory.createServer(new PaxosImpl());
		Paxos server4 = remotingFactory.createServer(new PaxosImpl());
		AsynchronousClient client = remotingFactory.createAsynchronousClient();
		
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
