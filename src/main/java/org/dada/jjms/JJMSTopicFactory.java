package org.omo.jjms;

public class JJMSTopicFactory implements JJMSDestinationFactory {

	@Override
	public JJMSDestination create(String name) {
		return new JJMSTopic(name);
	}

}
