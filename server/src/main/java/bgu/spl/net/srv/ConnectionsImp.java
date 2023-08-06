package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class ConnectionsImp<T> implements Connections<T> {
    public ConcurrentMap<Integer, ConnectionHandler> connections;
    public ConcurrentMap<String, List<Integer>> channelSubscribers;

    public ConcurrentMap<Integer, String> conIDToname;

    public ConcurrentMap<String, User> users;

    public static AtomicInteger connectionCounter;

    public static AtomicInteger messageIdCounter;

    private ConnectionsImp() {
        connections = new ConcurrentHashMap<Integer, ConnectionHandler>();
        channelSubscribers = new ConcurrentHashMap<String, List<Integer>>();
        conIDToname = new ConcurrentHashMap<Integer, String>();
        users = new ConcurrentHashMap<String, User>();
        connectionCounter = new AtomicInteger(0);
        messageIdCounter = new AtomicInteger(0);
    }

    private static ConnectionsImp instance = new ConnectionsImp<java.lang.String>();

    public static ConnectionsImp getInstance() {
        return instance;
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler handle = connections.get(connectionId);
        if (handle != null) {
            handle.send(msg);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void send(String channel, T msg) {
        int size = channelSubscribers.get(channel).size();
        for (int i = 0; i < size; i++) {
            int currConnection = channelSubscribers.get(channel).get(i);
            connections.get(currConnection).send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {

        // mysubsction = clear
        String name = conIDToname.get(connectionId);
        if (name != null) {
            User user = users.get(name);

            user.getmySubscribtion().clear();
            // update user logged in to false
            user.setLoggedIn(false);
            
            /////
            user.setConnectionID(-1);
            conIDToname.remove(connectionId);

            //// remove the connectionId from conToName

            // scan and remove all occurance of this client in channels
            for (Object key : channelSubscribers.keySet()) {
                if (channelSubscribers.get(key).contains(connectionId))
                    channelSubscribers.get(key).remove(connectionId);
            }
        }
        connections.remove(connectionId);
    }
}