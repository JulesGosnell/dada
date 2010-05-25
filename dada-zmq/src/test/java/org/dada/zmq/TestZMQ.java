package org.dada.zmq;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import junit.framework.TestCase;

public class TestZMQ extends TestCase {

	public TestZMQ(String name) {
		super(name);
		
		Runtime runtime = Runtime.getRuntime();
		//runtime.loadLibrary("zmq");
		//runtime.loadLibrary("jzmq");
	}

	public void testZMQ() throws Exception {
		
		//final String address = "tcp://lo:5555";
		final String address = "inproc://pipe";

		//final Context context = ZMQ.context(1, 1, 0);
//		final Socket socket = context.socket(ZMQ.REP);
//		socket.bind(address);
//		
//		Thread server = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				byte[] buffer = socket.recv(0);
//				assertTrue(new String(buffer).equals("hello"));
//				System.out.print(buffer);
//				socket.close();
//			}
//		});
//		server.start();
//		
//		Thread client = new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				Context context = ZMQ.context(1, 1, 0);
//				Socket socket = context.socket(ZMQ.REQ);
//				socket.connect(address);
//				byte[] buffer = "hello".getBytes();
//				socket.send(buffer, 0); // non-blocking mode
//				socket.close();
//			}
//		});
//		client.start();
//		
//		client.join();
//		server.join();
		
		//context.term(); // tidy up
	}
}
