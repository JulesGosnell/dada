package org.dada.core;

// TODO - collapse into SessionManager

public class SessionManagerHelper {

	public static Session SESSION = null; // TODO - HACK!

	public static void setCurrentSession(Session session) {
		SESSION = session;
	}

	public static Session getCurrentSession() {
		return SESSION;
	}

}
