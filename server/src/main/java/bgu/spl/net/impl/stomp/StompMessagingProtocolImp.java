package bgu.spl.net.impl.stomp;

import java.util.LinkedList;

//import javax.sql.rowset.CachedRowSet;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.*;

public class StompMessagingProtocolImp<T> implements StompMessagingProtocol<java.lang.String> {
    int connectionID;
    ConnectionsImp<java.lang.String> connectionsObj;
    boolean flagTerminate = false;
    boolean flagError = false;
    
    @Override
    public void start(int connectionId, ConnectionsImp<java.lang.String> connections) {

        this.connectionsObj = connections;
        this.connectionID = connectionId;
    }

    @Override
    public void process(java.lang.String message) {
        Frame frame = new Frame("NULL");

        frame.parseFrame(message);

        switch (frame.getCommand()) {
            case "CONNECT":
                caseConnect(frame);
                break;
            case "SUBSCRIBE":
                caseSubscribe(frame);
                break;
            case "UNSUBSCRIBE":
                caseUnsubscribe(frame);
                break;
            case "SEND":
                caseSend(frame);
                break;
            case "DISCONNECT":
                caseDisconnect(frame);
                break;
        }
    }

    // --------------------------------------------------------------------------------------------------
    public void caseConnect(Frame frame) {
        // Error Case 1 : Frame is not valid
        String checkFrame = frame.validConnect();
        if (checkFrame != "Valid") {
            createErrorFrame(checkFrame, checkFrame, frame);
            return;
        }

        java.lang.String username = frame.getValue("login");
        java.lang.String password = frame.getValue("passcode");
        User thisUser = connectionsObj.users.get(username);
        if (thisUser == null) {
            User user = new User(username, password, connectionID);
            connectionsObj.users.put(username, user);
        } else {
            // Error Case 3 : User already logged in
            if (thisUser.getLoggedIn()) {
                java.lang.String reason = "User already logged in";
                createErrorFrame(reason, checkFrame, frame);
                return;
            }
            if (thisUser.getPassword() != password) {
                String body = "Wrong password";
                createErrorFrame(body, checkFrame, frame);
                return;
            }
        }
        connectionsObj.conIDToname.put(connectionID, username);
        Frame connected = new Frame("CONNECTED");
        connected.addHeader("version", "1.2");
        connectionsObj.send(connectionID, connected.toString() + '\u0000');
    }

    // --------------------------------------------------------------------------------------------------
    public void caseSubscribe(Frame frame) {
        // Error Case 1 : Frame is not valid
        String checkFrame = frame.validSubscribe();
        if (checkFrame != "Valid") {
            createErrorFrame(checkFrame, checkFrame, frame);
            return;
        }
        // Error Case 2 : user is not loggedin
        String name = connectionsObj.conIDToname.get(connectionID);
        if (!connectionsObj.users.get(name).getLoggedIn()) {
            createErrorFrame("User is not loggedIn", checkFrame, frame);
            return;
        }
        // check if this channel exist
        String channel = frame.getValue("destination");
        if (connectionsObj.channelSubscribers.get(channel) == null) {
            // Add to connection subscrition list
            LinkedList<Integer> channelList = new LinkedList<Integer>();
            channelList.add(connectionID);
            connectionsObj.channelSubscribers.put(channel, channelList);
            // Add to users subrition list
            String subscritionId = frame.getValue("id");
            connectionsObj.users.get(name).getmySubscribtion().put(subscritionId, channel);
        } else {

            if (connectionsObj.channelSubscribers.get(channel).contains(connectionID)) {
                // Error(?) Case 3 : user already subscribed to this channel - TO CHECK
                return;
            }
            connectionsObj.channelSubscribers.get(channel).add(connectionID);
            String subscritionId = frame.getValue("id");
            connectionsObj.users.get(name).getmySubscribtion().put(subscritionId, channel);
            connectionsObj.users.get(name).getmySubscribtion().put(channel, subscritionId);
        }

        // creats new reciept frame
        Frame rFrame = new Frame("RECEIPT");
        String receiptId = frame.getValue("receipt");
        rFrame.addHeader("receipt-id", receiptId);
        connectionsObj.send(connectionID, rFrame.toString()+'\u0000');
    }

    // --------------------------------------------------------------------------------------------------
    public void caseUnsubscribe(Frame frame) {
        // Error Case 1 : Frame is not valid
        String checkFrame = frame.validUnSubscribe();
        if (checkFrame != "Valid") {
            createErrorFrame(checkFrame, checkFrame, frame);
            return;
        }
        // Error Case 2 : user is not logged in
        String name = connectionsObj.conIDToname.get(connectionID);
        if (!connectionsObj.users.get(name).getLoggedIn()) {
            createErrorFrame("User is not loggedIn", checkFrame, frame);
            return;
        }
        User user = connectionsObj.users.get(name);
        String subscritionId = frame.getValue("id");
        String channel = user.getmySubscribtion().get(subscritionId);

        // --------CHECK IF WE NEED TO RETURN ERROR!!!!!!!!!!!!!!!
        // Error Case 3 : user is not subscribe to channel
        String topic = frame.getValue("destination");
        Boolean flag;
        try {
            flag = connectionsObj.channelSubscribers.get(topic).contains(connectionID);
        } catch (Exception e) {
            flag = false;
        }

        if (!flag) {
            createErrorFrame("User is not subscribe to channel", checkFrame, frame);
            return;
        }
        // creats new reciept frame
        Frame rFrame = new Frame("RECEIPT");
        String receiptId = frame.getValue("receipt");
        rFrame.addHeader("receipt-id", receiptId);
        connectionsObj.send(connectionID, rFrame.toString()+'\u0000');
        connectionsObj.users.get(name).getmySubscribtion().remove(subscritionId, channel);//
        connectionsObj.users.get(name).getmySubscribtion().remove(channel, subscritionId);//

    }

    // --------------------------------------------------------------------------------------------------

    public void caseSend(Frame frame) {
        // Error Case 1 : Frame is not valid
        String checkFrame = frame.validSend();
        if (checkFrame != "Valid") {
            createErrorFrame(checkFrame, checkFrame, frame);
            return;
        }
        // Error Case 2 : user is not logged in
        String name = connectionsObj.conIDToname.get(connectionID);
        if (!connectionsObj.users.get(name).getLoggedIn()) {
            createErrorFrame("User is not loggedIn", checkFrame, frame);
            return;
        }

        User user = connectionsObj.users.get(name);
        String body = frame.getBody();
        // String channel = user.getmySubscribtion().get(subscritionId);
        String topic = frame.getValue("destination");

        // Error Case 3 : user is not subscribe to channel
        Boolean flag;
        try {
            flag = connectionsObj.channelSubscribers.get(topic).contains(connectionID);
        } catch (Exception e) {
            flag = false;
        }

        if (!flag) {
            createErrorFrame("User is not subscribe to channel", checkFrame, frame);
            return;
        }

        Integer messageId = ConnectionsImp.messageIdCounter.getAndIncrement();
        // to extract the topic of the message to send
        int sizeOfLoop = connectionsObj.channelSubscribers.get(topic).size();
        for (int i = 0; i < sizeOfLoop; i++) {
            int indexToSend = connectionsObj.channelSubscribers.get(topic).get(i);
            Frame messageFrame = new Frame("MESSAGE");

            messageFrame.addHeader("subscription", Integer.toString(indexToSend));
            messageFrame.addHeader("message-id", Integer.toString(messageId));
            messageFrame.addHeader("destination", topic);
            messageFrame.setBody(body);
            connectionsObj.send(indexToSend, messageFrame.toString()+'\u0000');
        }
    }

    // --------------------------------------------------------------------------------------------------
    //// Roie made
    public void caseDisconnect(Frame frame) {
        // Error Case 1 : Frame is not valid
        String checkFrame = frame.validDisconnect();
        if (checkFrame != "Valid") {
            createErrorFrame(checkFrame, checkFrame, frame);
            return;
        }
        // Error Case 2 : user is not logged in
        String name = connectionsObj.conIDToname.get(connectionID);
        if (!connectionsObj.users.get(name).getLoggedIn()) {
            createErrorFrame("User is not loggedIn", checkFrame, frame);
            return;
        }

        /// creats new reciept frame
        Frame rFrame = new Frame("RECEIPT");
        String receiptId = frame.getValue("receipt");
        rFrame.addHeader("receipt-id", receiptId);
        connectionsObj.send(connectionID, rFrame.toString()+'\u0000');
        User user = connectionsObj.users.get(name);
        user.setLoggedIn(false);
        flagTerminate=true;
    }

    // --------------------------------------------------------------------------------------------------
    // Used in caseConnect
    public void createErrorFrame(java.lang.String reason, String checkFrame, Frame clientFrame) {
        Frame frame = new Frame("ERROR");
        frame.addHeader("receipt-id", String.valueOf(ConnectionsImp.messageIdCounter.getAndIncrement()));
        String reasonToHeader = "";
        String body = "";
        if (checkFrame != "Valid") {
            reasonToHeader = "malformed frame received";
            body = "\n The message: \n ----- \n" + clientFrame.toString() + "\n ----- " + reason;
        } else {
            reasonToHeader = "You cannot do this oparation, more details will be provided at the body of the message";
            body = "\n" + reason;
        }
        frame.addHeader("message", reasonToHeader);
        frame.setBody(body);
        connectionsObj.send(connectionID, frame.toString()+'\u0000');
        connectionsObj.disconnect(connectionID);
        flagTerminate=true;
    
    }

    ///// using when sending message to chanel Roie made
    public Frame createMessageFrame(Frame frameSent) {
        //////
        String name = connectionsObj.conIDToname.get(connectionID);
        User user = connectionsObj.users.get(name);
        // id not A field - ??????
        String subscritionId = frameSent.getValue("id");
        String channel = user.getmySubscribtion().get(subscritionId);
        ///////
        Frame mframe = new Frame("MESSAGE");
        Integer messageId = ConnectionsImp.messageIdCounter.getAndIncrement();
        mframe.addHeader("subscription", subscritionId);
        mframe.addHeader("message-id ", messageId.toString());
        String dest = frameSent.getValue(channel);
        mframe.addHeader("destination", dest);
        mframe.setBody(frameSent.getBody());

        return mframe;
    }

    /**
     * @return true if the connection should be terminated
     */
    @Override
    public boolean shouldTerminate() {

        return flagTerminate;
    }

    public boolean getFlagError() {
        return flagError;
    }

}
