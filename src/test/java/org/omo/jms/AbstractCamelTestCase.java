package org.omo.jms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;

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

	protected Peer server;
	protected Peer client;
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
		server = new PeerImpl();
		string = "hello";
		mungedString = server.munge(string);
		jndiContext = new JndiContext();
		jndiContext.bind("Peer", server);
		camelContext = new DefaultCamelContext(jndiContext);
		connectionFactory = new CachingConnectionFactory(getConnectionFactory());
		camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
		camelContext.addRoutes(new RouteBuilder() {
		    public void configure() {
		        from("test-jms:queue:test.queue").to("bean:Peer");
		    }
		});

		long day = 1000 * 60 * 24;
		JmsEndpoint endpoint = (JmsEndpoint)camelContext.getEndpoint("test-jms:queue:test.queue");
		//endpoint.getConfiguration().setRequestTimeout(1 * day);
		client = ProxyHelper.createProxy(endpoint, Peer.class);
		//client = ProxyHelper.createProxy(camelContext.getEndpoint("bean:Peer"), Peer.class);
		camelContext.start();
		
		
	}

	protected void tearDown() throws Exception {
		camelContext.stop();
		super.tearDown();
		logger.info("end test");
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
