package org.dada.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Batch<V> {

	// TODO: this should not be at instance level
	private final Collection<Update<V>> EMPTY = Collections.emptyList();

	private Collection<Update<V>> insertions  = EMPTY;
	private Collection<Update<V>> alterations = EMPTY;
	private Collection<Update<V>> deletions   = EMPTY;
	
	public Collection<Update<V>> getInsertions() {
		return insertions;
	}

	public Collection<Update<V>> getAlterations() {
		return alterations;
	}

	public Collection<Update<V>> getDeletions() {
		return deletions;
	}
	
	public void addInsertion(Update<V> insertion) {
		if (insertions == EMPTY)
			insertions = new ArrayList<Update<V>>();
		insertions.add(insertion);
	}

	public void addAlteration(Update<V> alteration) {
		if (alterations == EMPTY)
			alterations = new ArrayList<Update<V>>();
		alterations.add(alteration);
	}

	public void addDeletion(Update<V> deletion) {
		if (deletions == EMPTY)
			deletions = new ArrayList<Update<V>>();
		deletions.add(deletion);
	}

}