package org.dada.core;

import java.util.Collection;

public interface SessionManager {

    String getName();
    Session createSession();
    void close() throws Exception;

}
