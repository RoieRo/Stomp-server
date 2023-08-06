package bgu.spl.net.srv;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    String userName;
    String password;
    boolean loggedIn;
    ConcurrentMap<String, String> mySubscribtion;
    ConcurrentMap<String, String> channelTosubID;
    int connectionID;

    public User(String newuserName,String newpassword, int newconnectionID){
        userName = newuserName;
        password = newpassword;
        loggedIn = true;
        connectionID = newconnectionID;
        mySubscribtion = new ConcurrentHashMap<String, String>(); //subscribtionID to channel
        channelTosubID = new ConcurrentHashMap<String, String>(); // channel to subscribtionID
    }

    // getters and setters
    public String getPassword() {
        return password;
    }
    public String getUserName() {
        return userName;
    }

    public boolean getLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean islogedIn)
    {
        loggedIn=islogedIn;
    }
    public int getConnectionID()
    {
        return connectionID;
    }

    public ConcurrentMap<String, String> getmySubscribtion()
    {
        return mySubscribtion;
    }
    public ConcurrentMap<String, String> getchannelTosubID()
    {
        return channelTosubID;
    }

    public void setConnectionID(int newConnectionId)
    {
        connectionID=newConnectionId;
    }
    
    
}
