package com.nomura.consensus.paxos;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;

public class Proposal<V> implements Serializable {

	private final int number;
	private final V value;
	
	public Proposal(int number, V value) {
		this.number = number;
		this.value = value;
	}
	
	public int getNumber() {
		return number;
	}
	
	public V getValue() {
		return value;
	}

	 public boolean equals(Object that) {
		   return EqualsBuilder.reflectionEquals(this, that);
	 }

}
