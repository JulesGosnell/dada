package com.nomura.cash;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class ManagerTestCase extends TestCase {

	protected int managerId;
	protected Identifiable identity;
	protected Manager<Identifiable, Identifiable> manager;

	public void setUp() throws Exception {
		managerId = 1;
		identity = new IdentifiableImpl(managerId);
		manager = new ManagerImpl<Identifiable, Identifiable>(identity);
	}

	public void tearDown() throws Exception {
		manager = null;
	}

	public void testId() {
		assertTrue(manager.getId() == managerId);
	}
	
	public void testSimpleUpdateAdd() {
		final int id1=0;
		assertTrue(manager.size()==0);
		Identifiable identifiable1 = new IdentifiableImpl(id1);
		manager.update(identifiable1);
		assertTrue(manager.size()==1);
		assertTrue(manager.fetch(id1)==identifiable1);
		// same id replaces
		final int id2=1;
		Identifiable identifiable2 = new IdentifiableImpl(id2);
		manager.update(identifiable2);
		assertTrue(manager.size()==2);
		assertTrue(manager.fetch(id1)==identifiable1);
		assertTrue(manager.fetch(id2)==identifiable2);
	}

	public void testMultipleUpdateAdd() {
		assertTrue(manager.size()==0);
		List<Identifiable> identifiables = new ArrayList<Identifiable>();
		final int id1=0;
		Identifiable identifiable1 = new IdentifiableImpl(id1);
		identifiables.add(identifiable1);
		final int id2=1;
		Identifiable identifiable2 = new IdentifiableImpl(id2);
		identifiables.add(identifiable2);
		manager.update(identifiables);
		assertTrue(manager.size()==2);
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(id1);
		ids.add(id2);
		assertEquals(identifiables, manager.fetch(ids));
		// TODO: should be Sets ?
	}

	public void testSimpleUpdateReplace() {
		final int id=0;
		assertTrue(manager.size()==0);
		Identifiable identifiable = new IdentifiableImpl(id);
		manager.update(identifiable);
		assertTrue(manager.size()==1);
		assertTrue(manager.fetch(id)==identifiable);
		// same id replaces
		identifiable = new IdentifiableImpl(id);
		manager.update(identifiable);
		assertTrue(manager.size()==1);
	}
	
	public void testMultipleEmpty() {
		assertTrue(manager.size()==0);
		List<Identifiable> identifiables = Collections.emptyList();
		manager.update(identifiables);
		assertTrue(manager.size()==0);
		assertTrue(manager.fetch(0)==null); // what if we look for something that doesn't exist...
	}
}
