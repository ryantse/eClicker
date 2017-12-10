package edu.ucsb.cs.cs184.elicker.eclicker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Ryan on 12/10/17.
 */

public class DeviceManager {
    public static String DEVICE_ID = "device_id";
    public static String DEVICE_TOKEN = "device_token";
    private static DeviceManager deviceManager = null;
    private SharedPreferences sharedPreferences;

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
        ConnectionManager connectionManager = ConnectionManager.getInstance();
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
        try {
            URL url = new URL("http://" + ConnectionManager.serverHost + "/mobile/register-device");
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String readLine = bufferedReader.readLine();
            bufferedReader.close();

            JSONObject registerData = new JSONObject(readLine);

            SharedPreferences.Editor editor = this.sharedPreferences.edit();
            editor.putString(DEVICE_ID, registerData.getString("deviceId"));
            editor.putString(DEVICE_TOKEN, registerData.getString("deviceToken"));

            // Ensure that the values are written to storage immediately.
            editor.commit();
        } catch (Exception e) {
            System.err.println("Failed to register device with server.");
        }
    }

    public void reregisterDevice() {
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
