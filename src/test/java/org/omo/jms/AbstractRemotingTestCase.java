package org.omo.jms;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import junit.framework.TestCase;

import org.omo.jms.AbstractCamelTestCase.Unserialisable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRemotingTestCase extends TestCase {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	protected ConnectionFactory connectionFactory;
	protected Connection connection;
	protected Session session;
	protected int timeout;
	
	protected abstract ConnectionFactory getConnnectionFactory();
	
	@Override
	protected void setUp() throws Exception {
		connectionFactory = getConnnectionFactory(); 
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
	
	public static class ServerException extends Exception{};

	public static interface Server {
		int hashcode(String string);
		void throwException() throws ServerException;
		Exception returnException();
		Object register(Client client, Object data);
	}

	public static interface Client {
		Object callback(Object data);
	}
	
	public static class ServerImpl implements Server {
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
		@Override
		public Object register(Client client, Object data) {
			return client.callback(data);
		}
	}

	public static class ClientImpl implements Client {
		public Object callback(Object data) {
			return data;
		}
	}
	
	public void testInvocationTypes() throws Exception {
		Queue queue = session.createQueue(Server.class.getCanonicalName());
		RemotingFactory<Server> factory = new RemotingFactory<Server>(session, Server.class, timeout);
		ExecutorService executorservice = new ThreadPoolExecutor(10, 100, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));
		final Server server = factory.createServer(new ServerImpl(), queue, executorservice);
		final Server client = factory.createSynchronousClient(queue, true);
		AsynchronousClient asynchronousClient = factory.createAsynchronousClient(queue, true);
		
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
	
	public void testReentrantInvocation() throws Exception {
		ExecutorService executorservice = new ThreadPoolExecutor(10, 100, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));

		Queue serverQueue = session.createQueue(Server.class.getCanonicalName());
		RemotingFactory<Server> serverFactory = new RemotingFactory<Server>(session, Server.class, timeout);
		final Server serverImpl = serverFactory.createServer(new ServerImpl(), serverQueue, executorservice);
		final Server serverProxy = serverFactory.createSynchronousClient(serverQueue, true);
		
		Client client = new ClientImpl(); 
		Queue clientQueue = session.createQueue(Client.class.getCanonicalName());
		RemotingFactory<Client> clientFactory = new RemotingFactory<Client>(session, Client.class, timeout);
		final Client clientImpl = clientFactory.createServer(client, clientQueue, executorservice);
		final Client clientProxy = clientFactory.createSynchronousClient(clientQueue, true);

		// call server passing client and data.
		// server calls client passing data
		// client returns data to server
		// server returns data to us...
		Object data = "data";
		assertTrue(data.equals(serverProxy.register(clientProxy, data)));
	}

	
	public void testSendAsyncReceive() throws Exception {
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		TemporaryQueue queue = session.createTemporaryQueue();
		MessageProducer producer = session.createProducer(queue);
		MessageConsumer consumer = session.createConsumer(queue);
		final CountDownLatch latch = new CountDownLatch(1);
		consumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message arg0) {
				try {
					logger.info(((ObjectMessage)arg0).getObject().toString());
				} catch (JMSException e) {
					// ignore
				}
				latch.countDown();
			}
		});
		ObjectMessage message = session.createObjectMessage();
		message.setObject(new Unserialisable());
		producer.send(message);
		assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
	}

}

