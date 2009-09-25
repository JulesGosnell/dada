/**
 * 
 */
package org.omo.core;

public class SwitchableFilter<V> implements Filter<V> {

	private boolean answer;
	
	public boolean getAnswer() {
		return answer;
	}

	public void setAnswer(boolean answer) {
		this.answer = answer;
	}

	@Override
	public boolean apply(V value) {
		return answer;
	}

}