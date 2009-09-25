package org.omo.core;

import java.util.Collections;

import junit.framework.TestCase;

public class FilteredModelViewTestCase extends TestCase {

	private SwitchableFilter<Datum> query;
	private FilteredModelView<Integer, Datum> view;
	
	protected void setUp() throws Exception {
		super.setUp();
		query = new SwitchableFilter<Datum>();
		view = new FilteredModelView<Integer, Datum>(null, null, query);
	}

	protected void tearDown() throws Exception {
		view = null;
		query = null;
		super.tearDown();
	}
	
	public void testView() {

		query.setAnswer(true);
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 0);

		// simple insertion
		Datum datum0 = new DatumImpl(0, 0);
		view.update(Collections.singleton(datum0));
		
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update existing current value
		Datum datum1 = new DatumImpl(0, 1);
		view.update(Collections.singleton(datum1));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum1);
		assertTrue(view.maps.historic.count() == 0);
		
		// out of sequence updates
		Datum datum2 = new DatumImpl(0, 2);
		Datum datum3 = new DatumImpl(0, 3);
		view.update(Collections.singleton(datum3));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		view.update(Collections.singleton(datum2));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		query.setAnswer(false);
		
		// rejected value
		Datum reject = new DatumImpl(1, 0);
		view.update(Collections.singleton(reject));
		 
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		// retire existing value
		Datum datum4 = new DatumImpl(0, 4);
		view.update(Collections.singleton(datum4));
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum4);
		
		// update a retired value
		Datum datum5 = new DatumImpl(0, 5);
		view.update(Collections.singleton(datum5));

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum5);

		// update a retired value out of sequence
		Datum datum6 = new DatumImpl(0, 6);
		Datum datum7 = new DatumImpl(0, 7);
		view.update(Collections.singleton(datum7));
		
		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);

		view.update(Collections.singleton(datum6));

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);
		
		// unretire retired value
		query.setAnswer(true);
		Datum datum8 = new DatumImpl(0, 8);
		view.update(Collections.singleton(datum8));
		
		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum8);
		assertTrue(view.maps.historic.count() == 0);
	}
	
}

