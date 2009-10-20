package org.omo.core;

import javax.jms.ConnectionFactory;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;

public class CamelTestCase extends TestCase {
	
	Munger munger;
	String string;
	String mungedString;
	JndiContext jndiContext;
	CamelContext camelContext;
	ConnectionFactory connectionFactory;

	protected void setUp() throws Exception {
		super.setUp();
		munger = new MungerImpl();
		string = "hello";
		mungedString = munger.munge(string);
		jndiContext = new JndiContext();
		jndiContext.bind("munger", munger);
		camelContext = new DefaultCamelContext(jndiContext);
		connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useShutdownHook=false");
		camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
		camelContext.start();
	}

	protected void tearDown() throws Exception {
		camelContext.stop();
		super.tearDown();
	}

	public static interface Munger {
		public String munge(String string);
		public String munge(Munger munger, String string);
	}
	
	public static class MungerImpl implements Munger {
		public String munge(String string) {
			System.out.println("munging: " + string);
			return string.toUpperCase();
		}
		public String munge(Munger munger, String string) {
			return munger.munge(string);
		}
	}
	
	public void testBeanToBeanOverQueue() throws Exception {

		camelContext.addRoutes(new RouteBuilder() {
		    public void configure() {
		        from("test-jms:queue:test.queue").to("bean:munger");
		    }
		});

		Munger proxy = ProxyHelper.createProxy(camelContext.getEndpoint("test-jms:queue:test.queue"), Munger.class);
		assertTrue(proxy.munge(string).equals(mungedString));
		 
		// can we migrate a proxy and still use it ?
		//assertTrue(proxy.munge(proxy, string).equals(mungedString));
	}
	
	// TODO: CAMEL proxies are not relocatable (Serialisable)... - can I replace their impl ?
	
}
