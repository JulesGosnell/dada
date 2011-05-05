package org.dada.core;


public interface SessionManager {

    String getName();
    Session createSession(String userName, String applicationName, String applicationVersion);
    void close() throws Exception;

}
