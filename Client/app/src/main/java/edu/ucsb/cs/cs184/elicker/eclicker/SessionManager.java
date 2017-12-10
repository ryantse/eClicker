package edu.ucsb.cs.cs184.elicker.eclicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.sql.Connection;

public class SessionManager {
    private static Session session = null;

    public static class Session {
        private String sessionToken;
        private String sessionId;

        private Session() {

        }

        public String getSessionToken() {
            return sessionToken;
        }

        public String getSessionId() {
            return sessionId;
        }
    }

    public static Session getCurrentSession() {
        return session;
    }

    public static void createSessionWithKeys(String key1, String key2, String sessionId, final SessionJoinCallback callback) {
        ConnectionManager connectionManager = ConnectionManager.getInstance();

        connectionManager.onMessageOnce(MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK in session join with keys");
                    SessionManager.session = new Session();
                    SessionManager.session.sessionId = messageReceived.getSessionId();
                    SessionManager.session.sessionToken = messageReceived.getSessionToken();
                    callback.onJoinSuccess();
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                    callback.onJoinFailure(messageReceived.getErrorMessage());
                }

            }
        });
        connectionManager.sendMessage(buildSessionMessage("sessionKey", key1, key2, "", sessionId));

    }

    public static void createSessionWithToken(String sessionId, String sessionToken, final SessionJoinCallback callback) {
        // Subscribe to SESSION_JOIN to receive errors caused by calling session join.
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        connectionManager.onMessageOnce(MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK in session join using token");
                    SessionManager.session = new Session();
                    SessionManager.session.sessionId = messageReceived.getSessionId();
                    SessionManager.session.sessionToken = messageReceived.getSessionToken();
                    callback.onJoinSuccess();
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                    callback.onJoinFailure(messageReceived.getErrorMessage());
                }
            }
        });

        connectionManager.sendMessage(buildSessionMessage("sessionToken", "", "", sessionToken, sessionId));
    }

    private static String buildSessionMessage(String authenticationType, String key1, String key2,
                                              String sessionToken, String sessionId) {
        try {
            JSONObject messageDataObj = new JSONObject()
                    .put("authenticationType", authenticationType)
                    .put("sessionKeys", new JSONArray().put(key1).put(key2))
                    .put("sessionToken", sessionToken)
                    .put("sessionId", sessionId);

            return  new JSONObject()
                    .put("messageType", "SESSION_JOIN")
                    .put("messageData", messageDataObj)
                    .toString();

        } catch (JSONException exception) {
            System.err.println(exception.toString());
            return "";
        }
    }

    public interface SessionJoinCallback {
        void onJoinSuccess();
        void onJoinFailure(String reason);
    }

}
