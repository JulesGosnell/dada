package org.omo.core;

import java.util.ArrayList;
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
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum0);
		assertTrue(view.maps.historic.count() == 0);

		// update existing current value
		Datum<Integer> datum1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum1)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum1);
		assertTrue(view.maps.historic.count() == 0);

		// out of sequence updates
		Datum<Integer> datum2 = new IntegerDatum(0, 2);
		Datum<Integer> datum3 = new IntegerDatum(0, 3);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum3)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum2)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		query.setAnswer(false);

		// rejected value
		Datum<Integer> reject = new IntegerDatum(1, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null ,reject)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 1);
		assertTrue(view.maps.current.valAt(0) == datum3);
		assertTrue(view.maps.historic.count() == 0);

		// retire existing value
		Datum<Integer> datum4 = new IntegerDatum(0, 4);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum4)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum4);

		// update a retired value
		Datum<Integer> datum5 = new IntegerDatum(0, 5);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum5)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum5);

		// update a retired value out of sequence
		Datum<Integer> datum6 = new IntegerDatum(0, 6);
		Datum<Integer> datum7 = new IntegerDatum(0, 7);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum7)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);

		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum6)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

		assertTrue(view.maps.current.count() == 0);
		assertTrue(view.maps.historic.count() == 1);
		assertTrue(view.maps.historic.valAt(0) == datum7);

		// unretire retired value
		query.setAnswer(true);
		Datum<Integer> datum8 = new IntegerDatum(0, 8);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum8)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());

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
		public void insert(Collection<Update<Datum<Integer>>> values) {
			for (Update<Datum<Integer>> value : values) {
				total += value.getNewValue().getId();
			}
		}

		@Override
		public void remove(Collection<Update<Datum<Integer>>> values) {
			for (Update<Datum<Integer>> value : values)
				total -= value.getOldValue().getId();
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
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum0)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(view.maps.historic.count() == 0);
		assertTrue(aggregator.getAggregate() == 0);

		// update existing current value
		Datum<Integer> datum1 = new IntegerDatum(0, 1);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum1)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		// out of sequence updates
		Datum<Integer> datum2 = new IntegerDatum(0, 2);
		Datum<Integer> datum3 = new IntegerDatum(0, 3);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum3)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum2)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		query.setAnswer(false);

		// rejected value
		Datum<Integer> reject = new IntegerDatum(1, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, reject)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		// retire existing value
		Datum<Integer> datum4 = new IntegerDatum(0, 4);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum4)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		// update a retired value
		Datum<Integer> datum5 = new IntegerDatum(0, 5);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum5)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		// update a retired value out of sequence
		Datum<Integer> datum6 = new IntegerDatum(0, 6);
		Datum<Integer> datum7 = new IntegerDatum(0, 7);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum7)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);


		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum6)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		// unretire retired value
		query.setAnswer(true);
		Datum<Integer> datum8 = new IntegerDatum(0, 8);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum8)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 0);

		// insert 2nd value
		Datum<Integer> datum9 = new IntegerDatum(1, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum9)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 1);

		// insert 3rd value
		Datum<Integer> datum10 = new IntegerDatum(2, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum10)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 3);

		// remove/retire 3rd value
		query.setAnswer(false);		
		Datum<Integer> datum11 = new IntegerDatum(2, 1);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum11)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 1);

		// reinsert 3rd value
		query.setAnswer(true);
		Datum<Integer> datum12 = new IntegerDatum(2, 2);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum12)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
		assertTrue(aggregator.getAggregate() == 3);

		// register an aggregator
		Aggregator<Integer, Datum<Integer>> aggregator2 = new IdAggregator();
		Registration<Integer, Datum<Integer>> registration = view.register(aggregator2);
		Collection<Update<Datum<Integer>>> insertions = new ArrayList<Update<Datum<Integer>>>();
		for (Datum<Integer> datum : registration.getData())
			insertions.add(new Update<Datum<Integer>>(null, datum));
		aggregator2.insert(insertions);
		assertTrue(aggregator2.getAggregate() == 3);

		Datum<Integer> datum13 = new IntegerDatum(3, 0);
		view.update(Collections.singleton(new Update<Datum<Integer>>(null, datum13)), new ArrayList<Update<Datum<Integer>>>(), new ArrayList<Update<Datum<Integer>>>());
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
