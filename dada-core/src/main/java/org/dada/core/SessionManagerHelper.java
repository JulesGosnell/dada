package org.dada.core;

public class SessionManagerHelper {

	private static final ThreadLocal<SessionManager> CURRENT_SESSION_MANAGER = new ThreadLocal<SessionManager>();

	public static void setCurrentSessionManager(SessionManager sessionManager) {
		//System.out.println("SETTING SM on " + Thread.currentThread().getId() + "to " + sessionManager);
		//CURRENT_SESSION_MANAGER.set(sessionManager);
		SESSION_MANAGER = sessionManager;
	}

	public static SessionManager getCurrentSessionManager() {
		//SessionManager sessionManager = CURRENT_SESSION_MANAGER.get();
		//System.out.println("GETTING SM from " + Thread.currentThread().getId() + "= " + sessionManager);
		return SESSION_MANAGER;
	}

	public static SessionManager SESSION_MANAGER = null; // TODO - HACK!
}
