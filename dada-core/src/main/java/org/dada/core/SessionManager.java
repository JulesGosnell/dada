package org.dada.core;


public interface SessionManager {

    String getName();
    Session createSession();
    void close() throws Exception;

}
