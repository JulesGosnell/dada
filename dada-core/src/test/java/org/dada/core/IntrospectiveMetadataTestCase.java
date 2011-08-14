package org.dada.core;

import org.jmock.integration.junit3.MockObjectTestCase;

public class IntrospectiveMetadataTestCase extends MockObjectTestCase {
	
	interface Test {
		
		int getKey(); // real getter
		int getIgnored(int value); // name looks like getter, but takes a param - so not a getter
		int dummy(); // not a getter
		int getException() throws Exception; // a getter that can throw a hard Exception
	};
	
	public void test() throws Exception {
		
		Creator<Test> creator = new Creator<Test>(){

			@Override
			public Test create(Object... args) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}
		};
		
		IntrospectiveMetadata<Integer, Test> metadata = new IntrospectiveMetadata<Integer, Test>(Test.class, creator, "Key");
		final Test test = mock(Test.class);

	}
}
