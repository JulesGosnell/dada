/**
 * 
 */
package com.nomura.consensus.jms;

public interface AsyncInvocationListener {

	void onResult(Object value);
	void onError(Exception exception);
	
}