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

import org.dada.slf4j.Logger;
import org.dada.slf4j.LoggerFactory;

/**
 * Abstract base for Models.
 * Supports concepts of data, metadata, views and view notification.
 *
 * @author jules
 *
 * @param <K>
 * @param <V>
 */
public abstract class AbstractModel<K, V> implements Model<K, V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final String name;
	protected final Metadata<K, V> metadata;
	private final Object viewsLock = new Object();

	protected volatile Collection<View<V>> views = new ArrayList<View<V>>();

	public AbstractModel(String name, Metadata<K, V> metadata) {
		this.name = name;
		this.metadata = metadata;
	}

	@Override
	public String getName() {
		return name;
	}

	public abstract Collection<V> getData();

	@Override
	public Metadata<K, V> getMetadata() {
		return metadata;
	}

	@Override
	public Registration<K, V> registerView(View<V> view) {
		synchronized (viewsLock) {
			//views = (IPersistentSet)views.cons(view);
			Collection<View<V>> newViews = new ArrayList<View<V>>(views);
			newViews.add(view);
			views = newViews;
			logger.debug("{}: registered view: {}", name, view);
		}
		Collection<V> values = getData();
		return new Registration<K, V>(metadata, new ArrayList<V>(values)); // TODO: hack - clojure containers not serialisable
	}

	@Override
	public Collection<V> deregisterView(View<V> view) {
		synchronized (viewsLock) {
			Collection<View<V>> newViews = new ArrayList<View<V>>(views);
			newViews.remove(view);
			views = newViews;
			logger.debug("" + this + " deregistered view:" + view + " -> " + views);
		}
		Collection<V> values = getData();
		return new ArrayList<V>(values); // TODO: hack - clojure containers not serialisable
	}

	protected void notifyUpdate(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		Collection<View<V>> snapshot = views;
		for (View<V> view : snapshot) {
			try {
				view.update(insertions, updates, deletions);
			} catch (Throwable t) {
				logger.error("error during view notification: {}", view, t);
			}
		}
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + name + ">";
	}

}
