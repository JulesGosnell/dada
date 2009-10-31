package org.omo.jjms;

public class Job implements Runnable {

	private final JJMSMessage message;
	private final JJMSDestination destination;
	
	Job(JJMSMessage message, JJMSDestination destination) {
		this.message = message;
		this.destination = destination;
	}

	@Override
	public void run() {
		destination.dispatch(message);
	}
	
}
