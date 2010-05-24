package org.dada.core;

import java.util.Collection;

import junit.framework.TestCase;

public class BatchTestCase extends TestCase {

	public void test() {
		Batch<Object> batch = new Batch<Object>();
		
		Update<Object> insertion = new Update<Object>(null, null);
		Update<Object> alteration = new Update<Object>(null, null);
		Update<Object> deletion = new Update<Object>(null, null);
		
		batch.addInsertion(insertion);
		batch.addInsertion(insertion);
		batch.addAlteration(alteration);
		batch.addAlteration(alteration);
		batch.addDeletion(deletion);
		batch.addDeletion(deletion);
		
		Collection<Update<Object>> insertions = batch.getInsertions();
		assertTrue(insertions.size() == 2);
		for (Update<Object> update : insertions) assertTrue(update == insertion);
		Collection<Update<Object>> alterations = batch.getAlterations();
		assertTrue(alterations.size() == 2);
		for (Update<Object> update : alterations) assertTrue(update == alteration);
		Collection<Update<Object>> deletions = batch.getDeletions();
		assertTrue(deletions.size() == 2);
		for (Update<Object> update : deletions) assertTrue(update == deletion);
		
	}
}
