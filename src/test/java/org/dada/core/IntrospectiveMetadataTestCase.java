package org.dada.core;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class IntrospectiveMetadataTestCase extends MockObjectTestCase {
	
	interface Test {
		
		int getKey(); // real getter
		int getIgnored(int value); // name looks like getter, but takes a param - so not a getter
		int dummy(); // not a getter
		int getException() throws Exception; // a getter that can throw a hard Exception
	};
	
	public void test() throws Exception {
		
		IntrospectiveMetadata<Integer, Test> metadata = new IntrospectiveMetadata<Integer, Test>(Test.class, "Key");
		final Test test = mock(Test.class);

		// check attribute names
		
		List<String> attributeNames = metadata.getAttributeNames();
		assertTrue(attributeNames.size() == 2);
		assertTrue(attributeNames.contains("Key"));
		assertTrue(attributeNames.contains("Exception"));
		
		// getKey...
		
		final int key = 1;
		
        checking(new Expectations(){{
            one(test).getKey();
            will(returnValue(key));
        }});
        
		assertTrue(metadata.getKey(test).equals(key));

        // getException - hard exception
        
		{
			final Exception exception = new Exception();

			checking(new Expectations(){{
				one(test).getException();
				will(throwException(exception));
			}});

			try {
				metadata.getAttributeValue(test, attributeNames.indexOf("Exception"));
				fail();
			} catch (Exception e) {
				assertTrue(((InvocationTargetException)e.getCause()).getTargetException() == exception);
			}

		}
	}
}
