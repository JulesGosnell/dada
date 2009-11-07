package org.omo.jms;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import junit.framework.TestCase;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;

// TODO: insert a ThreadingProxy, to thread dispatched calls, so reentrancy works.
// TODO: apply patch to allow serialisation of Proxies
// TODO: add 'final' to all interface signatures to tell Camel that these params are OUT only
// TODO: replace our JMS remoting with Camel's...

public abstract class AbstractCamelTestCase extends TestCase {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected Munger server;
	protected Munger client;
	protected String string;
	protected String mungedString;
	protected JndiContext jndiContext;
	protected CamelContext camelContext;
	protected ConnectionFactory connectionFactory;
	protected ExecutorService executorService = Executors.newFixedThreadPool(20);

	public abstract ConnectionFactory getConnectionFactory();
	
	protected void setUp() throws Exception {
		logger.info("start test");
		super.setUp();
		server = new MungerImpl();
		string = "hello";
		mungedString = server.munge(string);
		jndiContext = new JndiContext();
		jndiContext.bind("munger", server);
		camelContext = new DefaultCamelContext(jndiContext);
		connectionFactory = new CachingConnectionFactory(getConnectionFactory());
		camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
		camelContext.addRoutes(new RouteBuilder() {
		    public void configure() {
		        from("test-jms:queue:test.queue").to("bean:munger");
		    }
		});

		long day = 1000 * 60 * 24;
		JmsEndpoint endpoint = (JmsEndpoint)camelContext.getEndpoint("test-jms:queue:test.queue");
		//endpoint.getConfiguration().setRequestTimeout(1 * day);
		client = ProxyHelper.createProxy(endpoint, Munger.class);
		//client = ProxyHelper.createProxy(camelContext.getEndpoint("bean:munger"), Munger.class);
		camelContext.start();
		
		
	}

	protected void tearDown() throws Exception {
		camelContext.stop();
		super.tearDown();
		logger.info("end test");
	}

	public static class Unserialisable implements Serializable {
		private String serialisable = "unserialisable";
		
		public String toString() {
			return "<" + getClass().getSimpleName() + ": " + serialisable + ">"; 
		}

		private void writeObject(ObjectOutputStream out) throws IOException {
			throw new RuntimeException();
		}
	};
	
	public static interface Munger {
		public String munge(final String string);
		public String remunge(final Munger munger, final String string);
		public Unserialisable noop(final Unserialisable unserialisable);
		public int one(final Object object);
		
	}
	
	public static class MungerImpl implements Munger {
		public String munge(final String string) {
			System.out.println("munging: " + string);
			//new Exception().printStackTrace();
			return string.toUpperCase();
		}
		public String remunge(final Munger munger, final String string) {
			return munger.munge(string);
		}
		
		public Unserialisable noop(final Unserialisable unserialisable) {
			System.out.println("nooping: " + unserialisable);
			return unserialisable;
		}

		public int one(final Object object) {
			System.out.println("one: " + object);
			//new Exception().printStackTrace();
			return 1;
		}
	}
	
	public void testSimpleRoundTrip() throws Exception {
		assertTrue(client.munge(string).equals(mungedString));
	}

	public void testNestedRoundTrip() throws Exception {
		// can we migrate a proxy and still use it ?
		assertTrue(client.remunge(client, string).equals(mungedString));
		// TODO: CAMEL proxies are not relocatable (Serialisable)... - can I replace their impl ?
	}
	
	public void testOutboundUnserialisable() throws Exception {
		Unserialisable unserialisable = new Unserialisable();
		String serialisable = "serialisable";
		assertTrue(1 == server.one(serialisable));
		assertTrue(1 == client.one(serialisable));
		logger.info("serialisable OK");
		assertTrue(1 == server.one(unserialisable));
		assertTrue(1 == client.one(unserialisable));
		logger.info("unserialisable OK");
	}

	public void testCanWeAvoidBeingSerialised() throws Exception {
		Unserialisable unserialisable = new Unserialisable();
		assertTrue(unserialisable == server.noop(unserialisable));
		assertTrue(unserialisable == client.noop(unserialisable));
	}

	
}
