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
import java.util.concurrent.atomic.AtomicReference;

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
	protected final AtomicReference<Collection<View<V>>> views = new AtomicReference<Collection<View<V>>>(new ArrayList<View<V>>());

	protected final String name;
	protected final Metadata<K, V> metadata;

	public AbstractModel(String name, Metadata<K, V> metadata) {
		this.name = name;
		this.metadata = metadata;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Metadata<K, V> getMetadata() {
		return metadata;
	}

	// I'm holding my Views in an ArrayList because, whilst insert/delete involves a slow copy, iterate should be
	// nice and fast...
	
	@Override
	public Data<V> attach(View<V> view) {
		Collection<View<V>> oldViews;
		Collection<View<V>> newViews;
		do {
			oldViews = views.get();
			newViews = new ArrayList<View<V>>(oldViews);
			newViews.add(view);
		} while (!views.compareAndSet(oldViews, newViews));
		logger.debug("{}: registered view: {} -> {}", name, view, newViews);
		return getData();
	}

	@Override
	public Data<V> detach(View<V> view) {
		Collection<View<V>> oldViews;
		Collection<View<V>> newViews;
		do {
			oldViews = views.get();
			newViews = new ArrayList<View<V>>(oldViews);
			newViews.remove(view);
		} while (!views.compareAndSet(oldViews, newViews));
		logger.debug("{}: deregistered view: {} -> {}", name, view, newViews);
		return getData();
	}

	private final Collection<Update<V>> empty = Collections.emptyList();
	
	public void notifyUpdate(Collection<Update<V>> insertions, Collection<Update<V>> alterations, Collection<Update<V>> deletions) {
		insertions = (insertions == null) ? empty : insertions;
		alterations = (alterations == null) ? empty : alterations;
		deletions = (deletions == null) ? empty : deletions;
		Collection<View<V>> snapshot = views.get();
		for (View<V> view : snapshot) {
			if (insertions.size()==0 && alterations.size()==0 && deletions.size()==0)
				logger.warn("{}: sending empty event", name);
			try {
				view.update(insertions, alterations, deletions);
			} catch (Throwable t) {
				logger.error("error during view notification: {}", t, view);
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
	    return "0x" + Integer.toHexString(System.identityHashCode(this)) + ":" + name;
	}

}
