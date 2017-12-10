package edu.ucsb.cs.cs184.elicker.eclicker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class SessionManager {
    public static String SESSION_ID = "session_id";
    public static String SESSION_TOKEN = "session_token";
    private static SessionManager sessionManager = null;
    private SharedPreferences sharedPreferences;


    private SessionManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public static void Initialize(Context context) {
        if (sessionManager == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SessionManager.class.getName(), Context.MODE_PRIVATE);
            sessionManager = new SessionManager(sharedPreferences);
        }
    }

    public static SessionManager getSessionManager() {
        return sessionManager;
    }

    public String getSessionId() {
        return this.sharedPreferences.getString(SESSION_ID, null);
    }

    public String getSessionToken() {
        return this.sharedPreferences.getString(SESSION_TOKEN, null);
    }

    public void exitSession() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.remove(SESSION_ID);
        editor.remove(SESSION_TOKEN);
        editor.commit();
    }

    public void createSession(final String sessionId, String[] keys, final SessionJoinCallback callback) {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        connectionManager.onMessageOnce(ConnectionManager.MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                try {
                    switch (messageData.getString("status")) {
                        case "OK":
                            JSONObject sessionData = messageData.getJSONObject("statusExtended");
                            String sessionId = sessionData.getString("sessionId");
                            String sessionToken = sessionData.getString("sessionToken");

                            SharedPreferences.Editor editor = SessionManager.this.sharedPreferences.edit();
                            editor.putString(SESSION_ID, sessionId);
                            editor.putString(SESSION_TOKEN, sessionToken);
                            editor.commit();

                            callback.onSessionJoinSuccess();
                            break;

                        case "ERROR":
                            callback.onSessionJoinFailure(messageData.getJSONObject("statusExtended").getString("errorReason"));
                            break;
                    }
                } catch (JSONException exception) {
                    System.err.println("Malformed JSON object received.");
                }
            }
        });

        try {
            JSONObject messageData = new JSONObject()
                    .put("authenticationType", "sessionKey")
                    .put("sessionId", sessionId)
                    .put("sessionKeys", new JSONArray(keys));
            connectionManager.sendMessage(ConnectionManager.MessageType.SESSION_JOIN, messageData);
        } catch (JSONException exception) {
            System.err.println("Failed to create message data for SESSION_JOIN.");
        }
    }

    public void createSession(final String sessionId, String sessionToken, final SessionJoinCallback callback) {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        connectionManager.onMessageOnce(ConnectionManager.MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                try {
                    switch (messageData.getString("status")) {
                        case "OK":
                            JSONObject sessionData = messageData.getJSONObject("statusExtended");
                            String sessionId = sessionData.getString("sessionId");
                            String sessionToken = sessionData.getString("sessionToken");

                            SharedPreferences.Editor editor = SessionManager.this.sharedPreferences.edit();
                            editor.putString(SESSION_ID, sessionId);
                            editor.putString(SESSION_TOKEN, sessionToken);
                            editor.commit();

                            callback.onSessionJoinSuccess();
                            break;

                        case "ERROR":
                            callback.onSessionJoinFailure(messageData.getJSONObject("statusExtended").getString("errorReason"));
                            break;
                    }
                } catch (JSONException exception) {
                    System.err.println("Malformed JSON object received.");
                }
            }
        });

        try {
            JSONObject messageData = new JSONObject()
                    .put("authenticationType", "sessionToken")
                    .put("sessionId", sessionId)
                    .put("sessionToken", sessionToken);
            connectionManager.sendMessage(ConnectionManager.MessageType.SESSION_JOIN, messageData);
        } catch (JSONException exception) {
            System.err.println("Failed to create message data for SESSION_JOIN.");
        }
    }

    public interface SessionJoinCallback {
        void onSessionJoinSuccess();

        void onSessionJoinFailure(String failureReason);
    }

}
