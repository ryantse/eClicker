package edu.ucsb.cs.cs184.elicker.eclicker;
import java.net.*;
import java.io.*;
import org.json.*;

public class Main implements SessionManager.SessionJoinCallback {
    public static void main(String[] args) throws Exception {
        Main main = new Main();
    }

    public Main() throws Exception {
        URL conn = new URL("http://" + ConnectionManager.serverHost + "/mobile/register-device");
        URLConnection yc = conn.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()));
        String inputLine = in.readLine();
        in.close();
        DeviceIdentifier deviceIdentifier = registerDevice(inputLine);
        System.out.println(deviceIdentifier.toString());

        ConnectionManager connectionManager = ConnectionManager.getInstance();
        // add listener
        connectionManager.onMessageOnce(MessageType.DEVICE_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK");
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                }
            }
        });

        // send message to websocket
        connectionManager.sendMessage("{\"messageType\":\"DEVICE_JOIN\",\"messageData\":\"" + deviceIdentifier.deviceToken + "\"}");

        // add a listener for session join.
        connectionManager.onMessageOnce(MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK in session join");
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                }
            }
        });

        // scan qr codes and get 2 keys.
        String key1 = "771";
        String key2 = "afe";
        String sessionId = "0B026725";
        SessionManager.createSessionWithKeys(key1, key2, sessionId, this);

//        clientEndPoint.sendMessage();


        while (true) {
            Thread.yield();
        }
    }

    private static DeviceIdentifier registerDevice(String jsonString) throws JSONException{

        JSONObject obj = new JSONObject(jsonString);
        String deviceId = obj.getString("deviceId");
        String deviceToken = obj.getString("deviceToken");
        return new DeviceIdentifier(deviceId, deviceToken);
    }

    @Override
    public void onJoinSuccess() {
        SessionManager.createSessionWithToken(SessionManager.getCurrentSession().getSessionId(), SessionManager.getCurrentSession().getSessionToken(), this);
    }

    @Override
    public void onJoinFailure(String reason) {
        System.out.println(reason);
    }
}
