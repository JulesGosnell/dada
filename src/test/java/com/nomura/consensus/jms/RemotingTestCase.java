package com.nomura.consensus.jms;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.nomura.consensus.jms.AsyncInvocationListener;
import com.nomura.consensus.jms.QueueFactory;
import com.nomura.consensus.jms.RemotingFactory;
import com.nomura.consensus.jms.RemotingFactory.AsynchronousClient;


public class RemotingTestCase extends TestCase {

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

	public void testInvocation() throws Exception {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		QueueFactory queueFactory = new QueueFactory();
		int timeout = 5000;
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
			asynchronousClient.invoke(Server.class.getMethod("throwException", null), null, new AsyncInvocationListener(){

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
			asynchronousClient.invoke(Server.class.getMethod("returnException", null), null, new AsyncInvocationListener(){

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
		
		connection.stop();
		connection.close();
	}
}
