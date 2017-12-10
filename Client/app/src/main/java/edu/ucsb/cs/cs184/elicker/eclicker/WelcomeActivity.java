package edu.ucsb.cs.cs184.elicker.eclicker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


public class WelcomeActivity extends AppCompatActivity {
    private static final int CAMERA_ACCESS_REQUEST = 632;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_ui);

        final ConnectionManager connectionManager = ConnectionManager.getInstance();

        while (!connectionManager.isConnected()) {
            // Waiting for the connection to the server to be established.
            Thread.yield();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            final SessionManager.SessionJoinCallback sessionJoinCallback = new SessionManager.SessionJoinCallback() {
                @Override
                public void onSessionJoinSuccess() {
                    // Since we successfully joined a session, then we should move to the waiting screen.
                    Intent intent = new Intent(WelcomeActivity.this.getApplicationContext(), WaitingActivity.class);
                    WelcomeActivity.this.startActivity(intent);
                }

                @Override
                public void onSessionJoinFailure(String failureReason) {
                    SessionManager.getSessionManager().exitSession();

                    Intent intent = new Intent(WelcomeActivity.this.getApplicationContext(), QRscanActivity.class);
                    WelcomeActivity.this.startActivity(intent);
                }
            };

            DeviceManager.DeviceJoinCallback deviceJoinCallback = new DeviceManager.DeviceJoinCallback() {
                @Override
                public void onDeviceJoinSuccess() {
                    // Try to do a session join.
                    SessionManager.Initialize(WelcomeActivity.this.getApplicationContext());
                    SessionManager sessionManager = SessionManager.getSessionManager();
                    if (sessionManager.getSessionId() != null && sessionManager.getSessionToken() != null) {
                        sessionManager.createSession(sessionManager.getSessionId(), sessionManager.getSessionToken(), sessionJoinCallback);
                    } else {
                        Intent intent = new Intent(WelcomeActivity.this.getApplicationContext(), QRscanActivity.class);
                        WelcomeActivity.this.startActivity(intent);
                    }
                }

                @Override
                public void onDeviceJoinFailure(String failureReason) {
                    if (failureReason.equals("DEVICE_TOKEN_INVALID")) {
                        DeviceManager deviceManager = DeviceManager.getDeviceManager();
                        deviceManager.reregisterDevice();
                        deviceManager.deviceJoin(this);
                    }
                }
            };

            DeviceManager.Initialize(getApplicationContext());
            DeviceManager deviceManager = DeviceManager.getDeviceManager();
            deviceManager.deviceJoin(deviceJoinCallback);
        }
    }

    public void requestPermissions(View view) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_ACCESS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_ACCESS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this.getApplicationContext(), WelcomeActivity.class);
                    startActivity(intent);
                }
                return;
            }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
