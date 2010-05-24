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
package org.dada.consensus.paxos;

public interface Proposer<V> {

	// THOUGHTS:

	// we probably need e.g. Node<v> implements Acceptor<V>, Proposer<V>, Learner<V>....
	// the result of marshalling and demarshalling a Node is a JMSProxy to that Node
	// therefore every Node needs an associated Destination (temporary Queue?)

	// at startup everyone gets an Node  proxy that dispatches on a clusterwide topic
	// as you startup, you make an async invocation on this interface proposing List<Acceptor>{yourself} as the value (membership)
	// this triggers everyone who is listening into adding their current membership list to this value a re-proposing it....
	// when the dust settles everyone should should have a complete membership list
	// anytime a node becomes suspect (i.e. does not reply to a round quickly enough) the suspicious node can launch a membership round...


	// once membership is established we can start thinking about arranging partitions - or should that be done as part of the membership round ?


}
