package org.omo.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RemotingTestCase extends TestCase {

	private final Log log = LogFactory.getLog(getClass());
	
	protected ConnectionFactory connectionFactory;
	protected Connection connection;
	protected Session session;
	protected int timeout;
	
	@Override
	protected void setUp() throws Exception {
		connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		timeout = 5000;
	}

	@Override
	protected void tearDown() throws Exception {
		timeout = 0;
		session.close();
		session = null;
		connection.stop();
		connection.close();
		connection = null;
		connectionFactory = null;
	}
	
	static class ServerException extends Exception{};

	static interface Server {
		int hashcode(String string);
		void throwException() throws ServerException;
		Exception returnException();
	}
	
	static class ServerImpl implements Server {
		public int hashcode(String string) {
			return string.hashCode();
		}
		public void throwException() throws ServerException {
			throw new ServerException();
		}
		@Override
		public Exception returnException() {
			return new ServerException();
		}
	}

	public void testProxyRemotability() throws Exception {
		RemotingFactory<Server> factory = new RemotingFactory<Server>(session, Server.class, session.createTemporaryQueue(), timeout);
		Server server = factory.createServer(new ServerImpl());
		Server localClient = factory.createSynchronousClient();
		String foo = "foo";
		assertTrue(localClient.hashcode(foo) == server.hashcode(foo));
		
		log.info("MARSHALLING PROXY...");
		ByteArrayOutputStream baos =  new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(localClient);
		log.info("UNMARSHALLING PROXY...");
		Client.setCurrentSession(session);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Server remoteClient = (Server)ois.readObject();
		log.info("REUSING PROXY...");
		assertTrue(remoteClient.hashcode(foo) == server.hashcode(foo));
		log.info("...DONE");
	}
	
	public void testRemoteInvocation() throws Exception {
		QueueFactory queueFactory = new QueueFactory();
		RemotingFactory<Server> factory = new RemotingFactory<Server>(session, Server.class, queueFactory, timeout);

		final Server server = factory.createServer(new ServerImpl());
		final Server client = factory.createSynchronousClient();
		AsynchronousClient asynchronousClient = factory.createAsynchronousClient();
		
		{
			final String string = "test";
				assertEquals(server.hashcode(string), client.hashcode(string));
			
			final CountDownLatch latch = new CountDownLatch(1);
			asynchronousClient.invoke(Server.class.getMethod("hashcode", new Class[]{String.class}), new Object[]{string}, new AsyncInvocationListener(){

				@Override
				public void onError(Exception exception) {
					assertTrue(false);
				}

				@Override
				public void onResult(Object value) {
					assertEquals(server.hashcode(string), value);
					latch.countDown();
				}});
			latch.await(5, TimeUnit.SECONDS);
			
		}

		{
			try {
				client.throwException();
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof ServerException);
			}
			
			final CountDownLatch latch = new CountDownLatch(1);
			asynchronousClient.invoke(Server.class.getMethod("throwException", (Class<?>[])null), null, new AsyncInvocationListener(){

				@Override
				public void onError(Exception exception) {
					assertTrue(exception instanceof ServerException);
					latch.countDown();
				}

				@Override
				public void onResult(Object value) {
					assertTrue(false);
				}});
			latch.await(5, TimeUnit.SECONDS);
		}

		{
			assertTrue(client.returnException() instanceof ServerException);
			final CountDownLatch latch = new CountDownLatch(1);
			asynchronousClient.invoke(Server.class.getMethod("returnException", (Class<?>[])null), null, new AsyncInvocationListener(){

				@Override
				public void onError(Exception exception) {
					assertTrue(false);
				}

				@Override
				public void onResult(Object value) {
					assertTrue(value instanceof ServerException);
					latch.countDown();
				}});
			latch.await(5, TimeUnit.SECONDS);
		}
		
	}
}
