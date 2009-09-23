package org.omo.ltw;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LoadTimeWeavingTestCase extends TestCase {

	public void testLTW() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml", LoadTimeWeavingTestCase.class);

//		{
//			Person person = (Person) ctx.getBean("person");
//			person.getId();
//			person.getFather();
//			person.setFather(null);
//			person.getMother();
//			person.setMother(null);
//		}
		{
			Person father = new PersonImpl(0);
			Person mother = new PersonImpl(1);
			Person person = new PersonImpl(2);
			person.getId();
			person.setFather(father);
			person.getFather();
			person.setMother(mother);
			person.getMother();
		}

	}
}
