package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.widget.TextView;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by Dong on 12/8/17.
 */

public class QRscanActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener, SessionManager.SessionJoinCallback {

    private QRCodeReaderView qrCodeReaderView;
    private TextView myQRoutput;
    private String curSessionID;
    private boolean authPending;


    public static final String DEVICE_ID = "deviceID";
    public static final String DEVICE_TOKEN = "deviceToken";
    public static final String SESSION_ID = "sessionID";
    public static final String SESSION_TOKEN = "sessionToken";

    public static String deviceID;
    public static String deviceToken;
    public static String sessionID;
    public static String sessionToken;

    public static SharedPreferences sharedPreferences;

    private ArrayList<String> ReadQR = new ArrayList<String>(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        authPending = false;
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        // initialize deviceId, deviceToken, sessionID, and sessionToken to that stored in sharedPreferences.
        deviceID = sharedPreferences.getString(DEVICE_ID, "");
        deviceToken = sharedPreferences.getString(DEVICE_TOKEN, "");
        sessionID = sharedPreferences.getString(SESSION_ID, "");
        sessionToken = sharedPreferences.getString(SESSION_TOKEN, "");

        final ConnectionManager connectionManager = ConnectionManager.getInstance();
        ConnectionManager.MessageListener messageListener = new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK");
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                    connectionManager.onMessageOnce(MessageType.DEVICE_JOIN, this);
                    registerDevice();
                    connectionManager.sendMessage("{\"messageType\":\"DEVICE_JOIN\",\"messageData\":\""
                            + deviceToken + "\"}");
                }
            }
        };
        // need to wait deviceToken to get in
        connectionManager.onMessageOnce(MessageType.DEVICE_JOIN, messageListener);
        connectionManager.sendMessage("{\"messageType\":\"DEVICE_JOIN\",\"messageData\":\"" + deviceToken + "\"}");
        final QRscanActivity qRscanActivity = this;
        // listener for after we send QR data, we expect a response.
//        connectionManager.onMessageOnce(MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
//            @Override
//            public void handleMessage(MessageReceived messageReceived) {
//                if (messageReceived.getStatus().equals("OK")) {
//                    System.out.println("OK in session join");
//
//                } else if (messageReceived.getStatus().equals("ERROR")) {
//                    System.out.println(messageReceived.getErrorMessage());
//                }
//            }
//        });


        qrCodeReaderView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        myQRoutput = findViewById(R.id.instructionText);

        qrCodeReaderView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true);

        // Use this function to change the autofocus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(2000L);

        // Use this function to enable/disable Torch
        qrCodeReaderView.setTorchEnabled(true);

        // Use this function to set front camera preview
        //qrCodeReaderView.setFrontCamera();

        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera();
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        //myQRoutput.setText(text);
        //System.out.println(text);
        String[] parsed = text.split(":");
        curSessionID = parsed[0];
        checkThrowAway();

        if (ReadQR.size() == 3) {
            if (!authPending) {
                authPending = true;
                SessionManager.createSessionWithKeys(ReadQR.get(1), ReadQR.get(2), ReadQR.get(0), this);
            }
            //System.out.println(ReadQR);
            //Intent intent = new Intent(this, );
            //startActivity(intent);
            ReadQR.remove(1);
        }
        ReadQR.add(parsed[1]);
        if (ReadQR.size() == 3) {
            if (!authPending) {
                authPending = true;
                SessionManager.createSessionWithKeys(ReadQR.get(1), ReadQR.get(2), ReadQR.get(0), this);
            }
        }

    }

    public void checkThrowAway() {
        if (ReadQR.isEmpty()) {
            ReadQR.add(curSessionID);
        } else if (!ReadQR.get(0).equals(curSessionID)) {
            ReadQR.clear();
            ReadQR.add(curSessionID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeReaderView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeReaderView.stopCamera();
    }

    public void registerDevice() {
        try {
            URL conn = new URL("http://" + ConnectionManager.serverHost + "/mobile/register-device");
            URLConnection yc = conn.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    yc.getInputStream()));
            String inputLine = in.readLine();
            in.close();
            System.out.println(inputLine);
            JSONObject obj = new JSONObject(inputLine);
            String localDeviceId = obj.getString("deviceId");
            String localDeviceToken = obj.getString("deviceToken");
            deviceToken = localDeviceToken;
            deviceID = localDeviceId;
            SharedPreferences.Editor prefEditor = sharedPreferences.edit();
            prefEditor.putString(DEVICE_ID, localDeviceId);
            prefEditor.putString(DEVICE_TOKEN, localDeviceToken);
            prefEditor.commit();
        } catch (Exception e) {
            System.err.println("Error in registerDevice().");
        }
    }

    @Override
    public void onJoinSuccess() {
        // Move to new screen.
        System.out.println("In on join success");
        Intent intent = new Intent(QRscanActivity.this.getApplicationContext(), WaitingActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onJoinFailure(String reason) {
        authPending = false;
    }
}
