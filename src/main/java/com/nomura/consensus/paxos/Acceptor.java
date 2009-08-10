package com.nomura.consensus.paxos;

public interface Acceptor<V> {

	public abstract int prepare(int newNumber);

	public abstract boolean accept(Proposal<V> newProposal);

	public abstract int getNumber();

	public abstract Proposal<V> getProposal();

}