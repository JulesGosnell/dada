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

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// N.B. we shouldn't really be writing/reading files as part of the testsuite - but it's just the once...

public class FileCacheLoaderTestCase extends MockObjectTestCase {

	private final Logger logger = LoggerFactory.getLogger(getClass());

//  setImposteriser(ClassImposteriser.INSTANCE);
//  final File file = mock(File.class);
	
	public void testEquality() {
		assertEquals(Collections.emptyList(), Collections.emptyList());
		assertEquals(new Update<Integer>(null, 1), new Update<Integer>(null, 1));
	}
	
	public void testNoCachePresent() throws Exception {

		File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID() + ".ser");
		logger.info(file.toString());
		
        final Loader<Integer, Integer> nestedLoader = mock(Loader.class);
        final FileCacheLoader<Integer, Integer> loader = new FileCacheLoader<Integer, Integer>("test", null, file, nestedLoader);
        
        // no file cache available - so will call through to nested  loader...
		checking(new Expectations(){{
			one(nestedLoader).registerView(loader);
			one(nestedLoader).start();
			// we really need to send and check for an update through here...
			// TODO: how do we do that ?
			one(nestedLoader).stop();
		}});

		loader.start();
		loader.stop();

		assertTrue(file.exists());
		
		// TODO: uncomment when we have figured out how to generate events on demand...
		
//		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
//		assertTrue(ois.readInt() == 1);
//		assertTrue(ois.readObject().equals(1));
//		ois.close();
		
		file.delete();
		assertTrue(!file.exists());
	}

	public void testCachePresent() throws Exception {

		// prepare cache
		File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID() + ".ser");
		logger.info(file.toString());
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeInt(1);
		oos.writeObject(1);
		oos.close();
		assertTrue(file.exists());
		
        final Loader<Integer, Integer> nestedLoader = mock(Loader.class);
        final FileCacheLoader<Integer, Integer> loader = new FileCacheLoader<Integer, Integer>("test", null, file, nestedLoader);
        final View< Integer> view = mock(View.class);
        loader.registerView(view);

        // file cache is available - so will not call through to nested loader...
        // should read one update from file...
		checking(new Expectations(){{
			Collection<Update<Integer>> one = Collections.singleton(new Update<Integer>(null, 1));
			Collection<Update<Integer>> nil = Collections.emptyList();
			one(view).update(one, nil, nil);
		}});

		loader.start();
		loader.stop();

		file.delete();
		assertTrue(!file.exists());
	}
}
