package edu.ucsb.cs.cs184.eclicker;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.DecoderFactory;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class QRScanActivity extends AppCompatActivity implements BarcodeCallback, Handler.Callback {
    private final ArrayList<String> readQRSessionKeys = new ArrayList<>(2);
    private boolean sessionJoinPending = false;
    private String currentSessionId;
    private BarcodeView barcodeView;
    private Handler qrScanHandler;
    private int SCANNED_SESSION_DATA = 4242;
    private SessionManager.SessionJoinCallback sessionJoinCallback;
    private ConnectionManager.MessageListenerReference messageListenerReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        // Check if we have the necessary camera permission; if we don't, then go to the Camera Permission activity.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this.getApplicationContext(), CameraPermissionActivity.class);
            this.startActivity(intent);
            this.finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.qr_reader_toolbar);
        setSupportActionBar(toolbar);

        ConnectionManager connectionManager = ConnectionManager.getInstance().get();
        messageListenerReference = connectionManager.onMessageOnce(ConnectionManager.MessageType.GENERAL_NETWORK_DISCONNECTED, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                Intent intent = new Intent(QRScanActivity.this.getApplicationContext(), ReconnectActivity.class);
                QRScanActivity.this.startActivity(intent);
                QRScanActivity.this.finish();
            }
        });

        barcodeView = findViewById(R.id.qr_reader_view);

        // Use an optimized decoder factory that doesn't try to read all formats.
        barcodeView.setDecoderFactory(new DecoderFactory() {
            @Override
            public Decoder createDecoder(Map<DecodeHintType, ?> baseHints) {
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));

                MultiFormatReader reader = new MultiFormatReader();
                reader.setHints(hints);

                return new Decoder(reader);
            }
        });


        barcodeView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        barcodeView.autoFocus();
                        barcodeView.decodeContinuous(QRScanActivity.this);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        break;
                }

                return false;
            }
        });

        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setAutoFocusEnabled(false);
        barcodeView.setCameraSettings(cameraSettings);

        qrScanHandler = new Handler(this);

        sessionJoinCallback = new SessionManager.SessionJoinCallback() {
            @Override
            public void onSessionJoinSuccess() {
                // Since we are transitioning away from this activity, we are no longer interested in whether the network is disconnected.
                ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(QRScanActivity.this.getApplicationContext(), SessionWaitingActivity.class);
                        QRScanActivity.this.startActivity(intent);
                        QRScanActivity.this.finish();
                    }
                });
            }

            @Override
            public void onSessionJoinFailure(String failureReason) {
                System.err.printf("SESSION_JOIN failed: %s\n", failureReason);
                sessionJoinPending = false;
            }
        };

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr_scan, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_qr_code_device_id).setTitle(getResources().getString(R.string.menu_device_id, DeviceManager.getDeviceManager().getDeviceId()));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_qr_code_device_id:
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData copyText = ClipData.newPlainText("Device ID", DeviceManager.getDeviceManager().getDeviceId());

                if (clipboardManager != null) {
                    clipboardManager.setPrimaryClip(copyText);
                    Toast.makeText(this, R.string.toast_device_id_copied, Toast.LENGTH_LONG).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
        barcodeView.decodeContinuous(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void barcodeResult(BarcodeResult result) {
        String[] parsed = result.getText().split(":");

        if (parsed.length != 2) {
            return;
        }

        Bundle scannedSessionData = new Bundle();
        scannedSessionData.putString("sessionId", parsed[0]);
        scannedSessionData.putString("sessionKey", parsed[1]);

        Message scannedSessionDataMessage = Message.obtain(qrScanHandler, SCANNED_SESSION_DATA);
        scannedSessionDataMessage.setData(scannedSessionData);
        scannedSessionDataMessage.sendToTarget();
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what != SCANNED_SESSION_DATA) {
            return false;
        }

        Bundle scannedSessionData = msg.getData();
        String scannedSessionId = scannedSessionData.getString("sessionId");
        String scannedSessionKey = scannedSessionData.getString("sessionKey");

        if (!scannedSessionId.equals(currentSessionId)) {
            readQRSessionKeys.clear();
            currentSessionId = scannedSessionId;
        }

        // We want to ignore new messages of the same data.
        if(readQRSessionKeys.contains(scannedSessionKey)) {
            return true;
        }

        // We only want the latest two QR code keys.
        if (readQRSessionKeys.size() == 2) {
            readQRSessionKeys.remove(0);
        }

        // Add the latest QR code key.
        readQRSessionKeys.add(scannedSessionKey);

        if (readQRSessionKeys.size() >= 2 && !sessionJoinPending) {
            sessionJoinPending = true;
            SessionManager.getSessionManager().createSession(currentSessionId, new String[]{readQRSessionKeys.get(0), readQRSessionKeys.get(1)}, sessionJoinCallback);
        }
        return true;
    }
}
