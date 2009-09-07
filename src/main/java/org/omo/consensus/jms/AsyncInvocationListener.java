/**
 * 
 */
package org.omo.consensus.jms;

public interface AsyncInvocationListener {

	void onResult(Object value);
	void onError(Exception exception);
	
}