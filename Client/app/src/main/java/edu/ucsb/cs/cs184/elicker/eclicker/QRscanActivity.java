package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.content.Context;
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

public class QRscanActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    private QRCodeReaderView qrCodeReaderView;
    private TextView myQRoutput;
    private String curSessionID;

    public static final String DEVICE_ID = "deviceID";
    public static final String DEVICE_TOKEN = "deviceToken";
    public static final String SESSION_ID = "sessionID";
    public static final String SESSION_TOKEN = "sessionToken";

    public String savedDeviceID;
    public String savedDeviceToken;
    public String savedSessionID;
    public String savedSessionToken;

    SharedPreferences sharedPreferences;

    private ArrayList<String> ReadQR = new ArrayList<String>(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        savedDeviceID = sharedPreferences.getString(DEVICE_ID, null);
        savedDeviceToken = sharedPreferences.getString(DEVICE_TOKEN, null);
        savedSessionID = sharedPreferences.getString(SESSION_ID, null);
        savedSessionToken = sharedPreferences.getString(SESSION_TOKEN, null);

        ConnectionManager connectionManager = ConnectionManager.getInstance();
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
        connectionManager.sendMessage("{\"messageType\":\"DEVICE_JOIN\",\"messageData\":\"" + savedDeviceToken + "\"}");
        // Get the Intent that started this activity and extract the string
        //Intent intent = getIntent();

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

        if(ReadQR.size() == 3){
            //System.out.println(ReadQR);
            //Intent intent = new Intent(this, );
            //startActivity(intent);
            ReadQR.remove(1);
        }
        ReadQR.add(parsed[1]);
        if(ReadQR.size() == 3){

        }

    }

    public void checkThrowAway() {
        if(ReadQR.isEmpty()){
            ReadQR.add(curSessionID);
        }else if(ReadQR.get(0) != curSessionID){
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

    public void registerDevice() throws Exception {
        URL conn = new URL("http://" + ConnectionManager.serverHost + "/mobile/register-device");
        URLConnection yc = conn.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()));
        String inputLine = in.readLine();
        in.close();
        JSONObject obj = new JSONObject(inputLine);
        String deviceId = obj.getString("deviceId");
        String deviceToken = obj.getString("deviceToken");
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putString(DEVICE_ID, deviceId);
        prefEditor.putString(DEVICE_TOKEN, deviceToken);
        prefEditor.commit();
    }
}
