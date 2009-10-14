package org.omo.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import junit.framework.TestCase;
import clojure.lang.PersistentTreeMap;

public class FilteredModelViewTestCase extends TestCase {

	private SwitchableFilter<Datum<Integer>> query;
	private FilteredModelView<Integer, Datum<Integer>> view;

	protected void setUp() throws Exception {
		super.setUp();
		query = new SwitchableFilter<Datum<Integer>>();
		view = new FilteredModelView<Integer, Datum<Integer>>(null, null, query);
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
		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		view.update(Collections.singleton(datum0));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update existing current value
		Datum<Integer> datum1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(datum1));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum1);
		assertTrue(view.maps.historic.count() == 0);

		// out of sequence updates
		Datum<Integer> datum2 = new IntegerDatum(0, 2);
		Datum<Integer> datum3 = new IntegerDatum(0, 3);
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
		Datum<Integer> reject = new IntegerDatum(1, 0);
		view.update(Collections.singleton(reject));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		// retire existing value
		Datum<Integer> datum4 = new IntegerDatum(0, 4);
		view.update(Collections.singleton(datum4));

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum4);

		// update a retired value
		Datum<Integer> datum5 = new IntegerDatum(0, 5);
		view.update(Collections.singleton(datum5));

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum5);

		// update a retired value out of sequence
		Datum<Integer> datum6 = new IntegerDatum(0, 6);
		Datum<Integer> datum7 = new IntegerDatum(0, 7);
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
		Datum<Integer> datum8 = new IntegerDatum(0, 8);
		view.update(Collections.singleton(datum8));

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum8);
		assertTrue(view.maps.historic.count() == 0);
	}

	class IdAggregator implements Aggregator<Integer, Datum<Integer>> {

		int total = 0;

		@Override
		public Integer getAggregate() {
			return total;
		}

		@Override
		public void insert(Datum<Integer> value) {
			total += value.getId();
		}

		@Override
		public void remove(Datum<Integer> value) {
			total -= value.getId();
		}

		@Override
		public void update(Datum<Integer> oldValue, Datum<Integer> newValue) {
			total += newValue.getId() - oldValue.getId();
		}

	}

	public void testAggregator() {
		query.setAnswer(true);

		Aggregator<Integer, Datum<Integer>> aggregator = new IdAggregator();
		view.register(aggregator);

		assertTrue(view.maps.historic.count() == 0);
		assertTrue(aggregator.getAggregate() == 0);

		// simple insertion
		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		view.update(Collections.singleton(datum0));
		assertTrue(view.maps.historic.count() == 0);
		assertTrue(aggregator.getAggregate() == 0);

		// update existing current value
		Datum<Integer> datum1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(datum1));
		assertTrue(aggregator.getAggregate() == 0);

		// out of sequence updates
		Datum<Integer> datum2 = new IntegerDatum(0, 2);
		Datum<Integer> datum3 = new IntegerDatum(0, 3);
		view.update(Collections.singleton(datum3));
		assertTrue(aggregator.getAggregate() == 0);

		view.update(Collections.singleton(datum2));
		assertTrue(aggregator.getAggregate() == 0);

		query.setAnswer(false);

		// rejected value
		Datum<Integer> reject = new IntegerDatum(1, 0);
		view.update(Collections.singleton(reject));
		assertTrue(aggregator.getAggregate() == 0);

		// retire existing value
		Datum<Integer> datum4 = new IntegerDatum(0, 4);
		view.update(Collections.singleton(datum4));
		assertTrue(aggregator.getAggregate() == 0);

		// update a retired value
		Datum<Integer> datum5 = new IntegerDatum(0, 5);
		view.update(Collections.singleton(datum5));
		assertTrue(aggregator.getAggregate() == 0);

		// update a retired value out of sequence
		Datum<Integer> datum6 = new IntegerDatum(0, 6);
		Datum<Integer> datum7 = new IntegerDatum(0, 7);
		view.update(Collections.singleton(datum7));
		assertTrue(aggregator.getAggregate() == 0);


		view.update(Collections.singleton(datum6));
		assertTrue(aggregator.getAggregate() == 0);

		// unretire retired value
		query.setAnswer(true);
		Datum<Integer> datum8 = new IntegerDatum(0, 8);
		view.update(Collections.singleton(datum8));
		assertTrue(aggregator.getAggregate() == 0);

		// insert 2nd value
		Datum<Integer> datum9 = new IntegerDatum(1, 0);
		view.update(Collections.singleton(datum9));
		assertTrue(aggregator.getAggregate() == 1);

		// insert 3rd value
		Datum<Integer> datum10 = new IntegerDatum(2, 0);
		view.update(Collections.singleton(datum10));
		assertTrue(aggregator.getAggregate() == 3);

		// remove/retire 3rd value
		query.setAnswer(false);		
		Datum<Integer> datum11 = new IntegerDatum(2, 1);
		view.update(Collections.singleton(datum11));
		assertTrue(aggregator.getAggregate() == 1);

		// reinsert 3rd value
		query.setAnswer(true);
		Datum<Integer> datum12 = new IntegerDatum(2, 2);
		view.update(Collections.singleton(datum12));
		assertTrue(aggregator.getAggregate() == 3);

		// register an aggregator
		Aggregator<Integer, Datum<Integer>> aggregator2 = new IdAggregator();
		Registration<Integer, Datum<Integer>> registration = view.register(aggregator2);
		for (Datum<Integer> datum : registration.getData())
			aggregator2.insert(datum);
		assertTrue(aggregator2.getAggregate() == 3);

		Datum<Integer> datum13 = new IntegerDatum(3, 0);
		view.update(Collections.singleton(datum13));
		assertTrue(aggregator.getAggregate() == 6);
	}

	public void testDate() {
		int numDays = 5;
		PersistentTreeMap map = PersistentTreeMap.EMPTY;
		Collection<Date> dates = new DateRange(numDays).getValues();
		for (Date date : dates)
			map = map.assoc(date, date);
		assertTrue(map.count() == numDays);
		for (Date date : dates)
			assertTrue(map.get(date) == date);
	}
}
