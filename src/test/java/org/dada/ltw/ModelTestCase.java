package org.omo.ltw;

import java.lang.reflect.Field;

import junit.framework.TestCase;

public class ModelTestCase extends TestCase {

	static class ChangedEvent {
		Field field;
		Object newValue;
	}
	
	static class Address {
		int houseNumber;
	}
	
	static class Employee {
		String name;
		Address address;
	}
	
	
	public void testModel() {
		
		Address address = new Address();
		address.houseNumber = 100;
		Employee employee = new Employee();
		employee.address = address;
		employee.name = "jules";
		
		// how do we aspect these types to raise events when fields change ?
		// how do we register our interest in viewing these events ?
		
		
	}
}
