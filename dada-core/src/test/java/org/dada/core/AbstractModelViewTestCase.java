package org.dada.core;

import java.util.Collection;

import junit.framework.TestCase;

public class AbstractModelViewTestCase extends TestCase {

	public void testAbstractModelView() {
		
		String name = "modelView";
		Metadata<Integer, Datum<Integer>> metadata = null;
		
		AbstractModelView<Integer, Datum<Integer>> modelView = new AbstractModelView<Integer, Datum<Integer>>(name, metadata) {
			
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions,
					Collection<Update<Datum<Integer>>> alterations,
					Collection<Update<Datum<Integer>>> deletions) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public Data<Datum<Integer>> getData() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

			@Override
			public Datum<Integer> find(Integer key) {
				// TODO Auto-generated method stub
				// return null;
				throw new UnsupportedOperationException("NYI");
			}
		};

		modelView.start();
		modelView.stop();
		
	}
}
