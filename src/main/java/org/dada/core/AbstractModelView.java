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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractModelView<K, V> implements ModelView<K, V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final String name;
	protected final Metadata<K, V> metadata;
	private final Object viewsLock = new Object();
	protected volatile List<View<K, V>> views = new ArrayList<View<K,V>>();

	public AbstractModelView(String name, Metadata<K, V> metadata) {
		this.name = name;
		this.metadata = metadata;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getName() {
		return name;
	}

	public abstract Collection<V> getValues();

	@Override
	public Registration<K, V> registerView(View<K, V> view) {
		synchronized (viewsLock) {
			//views = (IPersistentSet)views.cons(view);
			List<View<K, V>> newViews = new ArrayList<View<K,V>>(views);
			newViews.add(view);
			views = newViews;
			logger.debug("{}: registered view: {}", name, view);
		}
		Collection<V> values = getValues();
		return new Registration<K, V>(metadata, new ArrayList<V>(values)); // TODO: hack - clojure containers not serialisable
	}

	@Override
	public boolean deregisterView(View<K, V> view) {
		try {
			synchronized (viewsLock) {
				//views = views.disjoin(view);
				List<View<K, V>> newViews = new ArrayList<View<K,V>>(views);
				newViews.remove(view);
				views = newViews;
				logger.debug("" + this + " deregistered view:" + view + " -> " + views);
			}
		} catch (Exception e) {
		    logger.error("unable to deregister view: {}", view);
		}
		return true;
	}

	protected void notifyUpdate(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		List<View<K, V>> snapshot = views;
		for (View<K, V> view : snapshot) {
			view.update(insertions, updates, deletions);
		}
	}

	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + name + ">";
	}

}
