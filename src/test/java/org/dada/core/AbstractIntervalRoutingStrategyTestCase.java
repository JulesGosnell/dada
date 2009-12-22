/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dada.core.Router.Strategy;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.joda.time.Interval;

public class AbstractIntervalRoutingStrategyTestCase extends MockObjectTestCase {

	private static class Event {
		private final Date date;
		
		private Event(Date date) {
			this.date = date;
		}
		
		private Date getDate() {
			return date;
		}
		
	}
	
	public void test() {
		Map<Interval, Collection<View<Integer, Event>>> map = new HashMap<Interval, Collection<View<Integer,Event>>>();
		
		long startInstant = System.currentTimeMillis();
		long endInstant = startInstant + 1;
		Interval interval = new Interval(startInstant, endInstant);

		
		Collection<View<Integer,Event>> views = new ArrayList<View<Integer,Event>>();
		View<Integer,Event> view = mock(View.class);
		views.add(view);
		
		map.put(interval, views);
		
		Strategy<Integer, Event> strategy = new AbstractIntervalRoutingStrategy<Event>(map) {
			@Override
			public boolean getMutable() {
				return true;
			}

			@Override
			public int getRoute(Event value) {
				return getRoute(value.getDate());
			}
		};
		
		Event before = new Event(new Date(startInstant - 1));
		Event onTime = new Event(new Date(startInstant));
		Event after = new Event(new Date(startInstant + 1));
		
		Collection<View<Integer, Event>> strategyViews = strategy.getViews(strategy.getRoute(onTime));
		assertTrue(strategyViews.size() == 1);
		assertTrue(strategyViews.iterator().next() == view);

		assertTrue(strategy.getViews(strategy.getRoute(before)).isEmpty());
		assertTrue(strategy.getViews(strategy.getRoute(after)).isEmpty());
	}
}
