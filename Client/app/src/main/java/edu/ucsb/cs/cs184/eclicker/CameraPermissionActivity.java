package edu.ucsb.cs.cs184.eclicker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

public class CameraPermissionActivity extends AppCompatActivity {
    private static final int CAMERA_ACCESS_REQUEST = 632;
    private ConnectionManager.MessageListenerReference messageListenerReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_permission);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this.getApplicationContext(), QRScanActivity.class);
            this.startActivity(intent);
            this.finish();
            return;
        }

        ConnectionManager connectionManager = ConnectionManager.getInstance().get();
        messageListenerReference = connectionManager.onMessageOnce(ConnectionManager.MessageType.GENERAL_NETWORK_DISCONNECTED, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                Intent intent = new Intent(CameraPermissionActivity.this.getApplicationContext(), ReconnectActivity.class);
                CameraPermissionActivity.this.startActivity(intent);
                CameraPermissionActivity.this.finish();
            }
        });

        Button grantButton = findViewById(R.id.camera_permission_grant);
        grantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPermissionActivity.this.requestPermissions();
            }
        });
    }

    public void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_ACCESS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_ACCESS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);

                    Intent intent = new Intent(this.getApplicationContext(), QRScanActivity.class);
                    startActivity(intent);
                    this.finish();
                }
                return;
            }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
