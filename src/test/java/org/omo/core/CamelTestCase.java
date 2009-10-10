package org.omo.core;

import javax.jms.ConnectionFactory;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;

public class CamelTestCase extends TestCase {
	
	private CamelContext camelContext;
	
	protected void setUp() throws Exception {
		super.setUp();
		camelContext = new DefaultCamelContext();
	}

	protected void tearDown() throws Exception {
		camelContext = null;
		super.tearDown();
	}

	public void testNothing() {
		
	}
	
	public void doNottestRemoting() throws Exception {
		
		camelContext.addRoutes(new RouteBuilder() {

		    public void configure() {
		        from("test-jms:queue:test.queue").to("file://test");
		        // set up a listener on the file component
		        from("file://test").process(new Processor() {

		            public void process(Exchange e) {
		                System.out.println("Received exchange: " + e.getIn());
		            }
		        });
		    }
		});
		
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		// Note we can explicity name the component
		camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
		ProducerTemplate template = camelContext.createProducerTemplate();
		camelContext.start();

		for (int i = 0; i < 10; i++) {
		    template.sendBody("test-jms:queue:test.queue", "Test Message: " + i);
		}

	}
}
