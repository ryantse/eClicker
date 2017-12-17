package edu.ucsb.cs.cs184.eclicker;

import android.content.Context;
import android.content.SharedPreferences;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Ryan on 12/10/17.
 */

public class DeviceManager {
    public static final String DEVICE_ID = "device_id";
    public static final String DEVICE_TOKEN = "device_token";
    private static DeviceManager deviceManager = null;
    private final SharedPreferences sharedPreferences;

    public DeviceManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;

        // Check if we have a device ID/token yet.
        if (!this.sharedPreferences.contains(DEVICE_ID) || !this.sharedPreferences.contains(DEVICE_TOKEN)) {
            this.registerDevice();
        }
    }

    public static void Initialize(Context context) {
        if (deviceManager == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(DeviceManager.class.getName(), Context.MODE_PRIVATE);
            deviceManager = new DeviceManager(sharedPreferences);
        }
    }

    public static DeviceManager getDeviceManager() {
        return deviceManager;
    }

    public String getDeviceId() {
        return this.sharedPreferences.getString(DEVICE_ID, null);
    }

    public String getDeviceToken() {
        return this.sharedPreferences.getString(DEVICE_TOKEN, null);
    }

    public void deviceJoin(final DeviceJoinCallback callback) {
        ConnectionManager connectionManager = ConnectionManager.getInstance().get();

        connectionManager.onMessageOnce(ConnectionManager.MessageType.DEVICE_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                try {
                    switch (messageData.getString("status")) {
                        case "OK":
                            callback.onDeviceJoinSuccess();
                            break;

                        case "ERROR":
                            callback.onDeviceJoinFailure(messageData.getJSONObject("statusExtended").getString("errorReason"));
                            break;
                    }
                } catch (JSONException exception) {
                    System.err.println("Malformed JSON object received.");
                }
            }
        });

        try {
            JSONObject messageData = new JSONObject();
            messageData.put("deviceToken", getDeviceToken());
            connectionManager.sendMessage(ConnectionManager.MessageType.DEVICE_JOIN, messageData);
        } catch (JSONException exception) {
            System.err.println("Failed to create message data for DEVICE_JOIN.");
        }
    }

    private void registerDevice() {
        SyncHttpClient syncHttpClient = new SyncHttpClient();
        syncHttpClient.get("http://" + ConnectionManager.serverHost + "/mobile/register-device", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                try {
                    SharedPreferences.Editor editor = DeviceManager.this.sharedPreferences.edit();
                    editor.putString(DEVICE_ID, response.getString("deviceId"));
                    editor.putString(DEVICE_TOKEN, response.getString("deviceToken"));
                    editor.commit();
                } catch (JSONException exception) {
                    System.err.println("Malformed JSON object received.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                System.err.println("Failed to receive proper response from server trying to register device.");
            }
        });
    }

    public void reregisterDevice() {
        // In order to reregister the device, we just clear out the current Device ID and Token and request for a new one.
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.remove(DEVICE_ID);
        editor.remove(DEVICE_TOKEN);
        editor.commit();

        this.registerDevice();
    }

    interface DeviceJoinCallback {
        void onDeviceJoinSuccess();

        void onDeviceJoinFailure(String failureReason);
    }
}
