package bgu.spl.net.impl.stomp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Frame {
    private String command;
    private ConcurrentHashMap<String, String> headers;
    private String body;

    public Frame(String newcommand) {
        command = newcommand;
        headers = new ConcurrentHashMap<String, String>();
    }

    public String getCommand() {
        return command;
    }

    public ConcurrentHashMap<String, String> getHeaders() {
        return headers;
    }

    public String getValue(String key) {
        String value = headers.get(key);
        if (value != null)
            return value.toString();
        return "";
    }

    public void addHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    public String getBody() {
        if (body != null)
            return body.toString();
        return "";
    }

    public void setBody(String frameBody) {
        body = frameBody;
    }

    public java.lang.String toString() {
        StringBuilder frameAsString = new StringBuilder();

        // Append the command and newline
        frameAsString.append(this.getCommand()).append("\n");

        // Append headers(if any)
        if (headers == null || headers.isEmpty()) // There are no headers, 0 'will be added instead
        {
            frameAsString.append("0");
        } else {
            for (Map.Entry<String, String> entry : this.getHeaders().entrySet()) {
                frameAsString.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }
        }
        // Append a blank line to indicate the end of the headers
        frameAsString.append("\n");

        // Append the body (if any)
        java.lang.String body = this.getBody();
        if (body != null && !body.isEmpty()) {
            frameAsString.append(body);
        }
        // a null char to indicate the end of this Frame will be appended in the encode
        // method
        java.lang.String str = frameAsString.toString();
        return str;
    }

    // To activate this method we will create a new frame
    public void parseFrame(String frameAsString) {
        // Split the string by newline characters
        String[] lines = frameAsString.split("\n");

        // The first line is the command
        String newcommand = lines[0].toString().trim();
        this.command = newcommand;

        if (!command.equals("SEND")) {
            // Parse the headers
            int lineIndex = 1;
            String line = lines[lineIndex];
            while ((!line.isEmpty() || !line.equals('\0')) && lineIndex < lines.length) {
                line = lines[lineIndex];
                String[] headerParts = line.split(":");
                String headerName = headerParts[0].toString();// .trim()
                // if(headerParts.length>1){

                // }
                String headerValue = headerParts[1].toString();// .trim()
                addHeader(headerName, headerValue);
                lineIndex++;
            }

            // The rest of the lines (if any) make up the body
            StringBuilder bodyBuilder = new StringBuilder();
            for (int i = lineIndex + 1; i < lines.length - 1; i++) {
                bodyBuilder.append(lines[i]).append("\n");
            }
            String body = bodyBuilder.toString().trim(); // trim removes unnececary white spaces
            if (!body.isEmpty()) {
                setBody(body);
            }
        } else {
            String[] headerParts = lines[1].split(":");
            String headerName = headerParts[0].toString();// .trim()
            String headerValue = headerParts[1].toString();// .trim()
            addHeader(headerName, headerValue);
            StringBuilder bodyBuilder = new StringBuilder();
            for (int i = 2; i < lines.length; i++) {
                bodyBuilder.append(lines[i] + "\n");
            }
            String body = bodyBuilder.toString(); // trim removes unnececary white spaces
            if (!body.isEmpty()) {
                setBody(body);
            } 
        }
    }

    // Valid method
    // --------------------------------------------------------------------------------------------------------------------------------
    public String validConnect() {
        if ((headers.get("accept-version") != null) && !headers.get("accept-version").equals("1.2")) {
            //System.out.println("Invalid version");
            return "Invalid version";
        }
        if (headers.get("host") == null) {
           // System.out.println("Did not contain a host");
            return "Did not contain a host";
        }
        if (headers.get("login") == null) {
            //System.out.println("Did not contain login field");
            return "Did not contain login field";
        }
        if (headers.get("passcode") == null) {
            //System.out.println("Did not contain passcode field");

            return "Did not contain password field";
        }
        return "Valid";
    }

    public String validSubscribe() {

        if (headers.get("destination") == null) {
            return "Did not contain destination";
        }
        if (headers.get("id") == null) {
            return "Did not contain id";
        }
        if (headers.get("receipt") == null) {
            return "Did not contain receipt";
        }
        return "Valid";
    }

    public String validUnSubscribe() {
        if (headers.get("id") == null) {
            return "Did not contain id";
        }
        if (headers.get("receipt") == null) {
            return "Did not contain receipt";
        }
        return "Valid";
    }

    public String validSend() {
        if (headers.get("destination") == null) {
            return "Did not contain destination";
        }
        return "Valid";
    }

    public String validDisconnect() {
        if (headers.get("receipt") == null) {
            return "Did not contain receipt";
        }
        return "Valid";
    }
}
