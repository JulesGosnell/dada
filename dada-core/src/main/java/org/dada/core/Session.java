package org.dada.core;

public interface Session extends SessionManager {

    boolean ping();
    long getLastPingTime();
    void close() throws Exception;
 
    Data<Object> attach(Model<Object, Object> model, View<Object> view);
    Data<Object> detach(Model<Object, Object> model, View<Object> view);
    Model<Object, Object> find(Model<Object, Object> model, Object key);
    Data<Object> getData(Model<Object, Object> model);

    Model<Object, Object> query(String query);
}
