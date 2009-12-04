package org.omo.jms;

import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.omo.core.Model;

public class SimpleMethodMapperTestCase extends TestCase {

	public void testMapping() {
		MethodMapper<Integer> mapper = new SimpleMethodMapper(Model.class);
		
		for (Method method : Model.class.getMethods()) {
			assertTrue(method.equals(mapper.getMethod(mapper.getKey(method))));
		}
	}
	
}
