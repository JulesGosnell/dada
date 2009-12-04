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

import java.util.Collection;

import org.dada.core.IntegerDatum;
import org.dada.core.IntrospectiveMetadata;
import org.dada.core.Metadata;
import org.dada.core.Update;
import org.dada.core.View;

import junit.framework.TestCase;

public class ViewTestCase extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		datumMetadata = new IntrospectiveMetadata<Integer, BooleanDatum>(BooleanDatum.class, "Id");
		stringDatumMetadata = new IntrospectiveMetadata<Integer, StringDatum>(StringDatum.class, "Id");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	static class BooleanDatum extends IntegerDatum {

		final boolean flag;

		BooleanDatum(int id, boolean flag) {
			super(id, 0);
			this.flag = flag;
		}

		public boolean getFlag() {
			return flag;
		}

	};

	protected Metadata<Integer, BooleanDatum> datumMetadata;
	protected Metadata<Integer, StringDatum> stringDatumMetadata;

	static class Counter<K, V> implements View<K, V> {

		int count;

		@Override
		public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
			count += insertions.size();
		}


	};

	public void testEmpty() {

	}

	// TODO: split Query into Filter/Transformer/Aggregator...
	// TODO: how do we support modification/removal using this architecture ?
	// TODO: modification will require exlusion/versioning
	// TODO: removal will require a deleted flag/status

}
