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
import java.util.Collections;

import org.dada.slf4j.Logger;
import org.dada.slf4j.LoggerFactory;

public class FilteredView<V> implements View<V> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public interface Filter<V> {
		boolean filter(V value);
	}

	private final Filter<V> filter;
	private final Collection<View<V>> views;
	
	public FilteredView(Filter<V> filter, Collection<View<V>> views) {
		this.filter = filter;
		this.views = views;
	}
	
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> alterations, Collection<Update<V>> deletions) {
		if (insertions.size()==0 && alterations.size()==0 && deletions.size()==0)
			logger.warn("{}: receiving empty event", new Exception(), "FilteredView");
		
		Collection<Update<V>> i = Collections.emptyList();
		for (Update<V> insertion : insertions) {
			if (filter.filter(insertion.getNewValue())) {
				if (i.isEmpty()) i = new ArrayList<Update<V>>();
				i.add(insertion);
			}
		}
		Collection<Update<V>> a = Collections.emptyList();
		for (Update<V> update : alterations) {
			if (filter.filter(update.getOldValue())) {
				if (a.isEmpty()) a = new ArrayList<Update<V>>();
				a.add(update);
			}
		}
		Collection<Update<V>> d = Collections.emptyList();
		for (Update<V> deletion : deletions) {
			if (filter.filter(deletion.getOldValue())) {
				if (d.isEmpty()) d = new ArrayList<Update<V>>();
				d.add(deletion);
			}
		}

		if (i.size() > 0 || a.size() > 0 || d.size() > 0) {
			for (View<V> view: views) {
				try {
					view.update(i, a, d);
				} catch (Throwable t) {
					logger.error("problem updating View: ", t);
				}
			}
		}
	}

}
