package org.dada.core;

public interface Session extends SessionManager {

    int ping();
    long getLastPingTime();
    @Override void close() throws Exception;

    String getUserName();
    String getApplicationName();
    String getApplicationVersion();
 
    Data<Object> attach(Model<Object, Object> model, View<Object> view);
    Data<Object> detach(Model<Object, Object> model, View<Object> view);
    Model<Object, Object> find(Model<Object, Object> model, Object key);
    Data<Object> getData(Model<Object, Object> model);

    Model<Object, Object> query(String query);
}
