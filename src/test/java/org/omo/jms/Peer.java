/**
 * 
 */
package org.omo.jms;


public interface Peer {
	
	int hashcode(final String string);
	void throwException() throws PeerException;
	Exception returnException();
	Object register(final Peer peer, final Object data);
	
	String munge(final String string);
	String remunge(final Peer peer, final String string);
	Unserialisable noop(final Unserialisable unserialisable);
	int one(final Object object);
	
	Object callback(final Object data);
	
}