package edu.ucsb.cs.cs184.elicker.eclicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import static edu.ucsb.cs.cs184.elicker.eclicker.QRscanActivity.SESSION_ID;
import static edu.ucsb.cs.cs184.elicker.eclicker.QRscanActivity.SESSION_TOKEN;

public class WelcomeActivity extends AppCompatActivity implements SessionManager.SessionJoinCallback{

    private static final int MY_PERMISSIONS_REQUEST = 632;

    private Button acceptButton;

    private int permissionCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_ui);

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        final WelcomeActivity welcomeActivity = this;
        // try to connect using existing session token.
        final ConnectionManager connectionManager = ConnectionManager.getInstance();
        while(!connectionManager.isConnected()) {
            Thread.yield();
        }
        ConnectionManager.MessageListener messageListener = new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK");
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                    connectionManager.onMessageOnce(MessageType.DEVICE_JOIN, this);
                    QRscanActivity.registerDevice();
                    connectionManager.sendMessage("{\"messageType\":\"DEVICE_JOIN\",\"messageData\":\""
                            + deviceToken + "\"}");
                }
            }
        };
        connectionManager.onMessageOnce(MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(MessageReceived messageReceived) {
                System.out.println("In Welcome Activity");
                System.out.println("Session Token " + messageReceived.getSessionToken());
                if (messageReceived.getStatus().equals("OK")) {
                    System.out.println("OK in session join");
                    Intent intent = new Intent(welcomeActivity, WaitingActivity.class);
                    startActivity(intent);
                    // TODO Change screen to question waiting or the current question.
                } else if (messageReceived.getStatus().equals("ERROR")) {
                    System.out.println(messageReceived.getErrorMessage());
                    if(permissionCheck == PackageManager.PERMISSION_GRANTED){
                        Intent intent = new Intent(welcomeActivity, QRscanActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("eclicker", Context.MODE_PRIVATE);
        String savedSessionID = sharedPreferences.getString(SESSION_ID, "");
        String savedSessionToken = sharedPreferences.getString(SESSION_TOKEN, "");
        System.out.println("savedSessionToken:" + savedSessionToken);
        SessionManager.createSessionWithToken(savedSessionID, savedSessionToken, this);
    }

    public void givePerms(View view){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            Intent intent = new Intent(this, QRscanActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    Intent intent = new Intent(this, QRscanActivity.class);
                    startActivity(intent);

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onJoinSuccess() {

    }

    @Override
    public void onJoinFailure(String reason) {

    }
}
