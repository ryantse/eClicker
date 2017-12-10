package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.util.ArrayList;

/**
 * Created by Dong on 12/8/17.
 */

public class QRscanActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {
    private SessionManager sessionManager;
    private QRCodeReaderView qrCodeReaderView;
    private boolean sessionJoinPending;
    private ArrayList<String> readQRSessionKeys = new ArrayList<String>(2);
    private String currentSessionId = "";
    private SessionManager.SessionJoinCallback sessionJoinCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        SessionManager.Initialize(getApplicationContext());
        sessionManager = SessionManager.getSessionManager();

        sessionJoinPending = false;

        qrCodeReaderView = findViewById(R.id.qrdecoderview);
        qrCodeReaderView.setOnQRCodeReadListener(this);
        qrCodeReaderView.setQRDecodingEnabled(true);
        qrCodeReaderView.setAutofocusInterval(2000L);
        qrCodeReaderView.setTorchEnabled(false);
        qrCodeReaderView.setBackCamera();

        sessionJoinCallback = new SessionManager.SessionJoinCallback() {
            @Override
            public void onSessionJoinSuccess() {
                qrCodeReaderView.setQRDecodingEnabled(false);
                Intent intent = new Intent(QRscanActivity.this.getApplicationContext(), WaitingActivity.class);
                startActivity(intent);
                QRscanActivity.this.finish();
            }

            @Override
            public void onSessionJoinFailure(String failureReason) {
                System.err.printf("SESSION_JOIN failed: %s\n", failureReason);
                sessionJoinPending = false;
            }
        };
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        String[] parsed = text.split(":");

        if (!currentSessionId.equals(parsed[0])) {
            readQRSessionKeys.clear();
            currentSessionId = parsed[0];
        }

        if (readQRSessionKeys.size() == 2) {
            readQRSessionKeys.remove(0);
        }

        readQRSessionKeys.add(parsed[1]);

        if (readQRSessionKeys.size() == 2 && !sessionJoinPending) {
            sessionJoinPending = true;
            sessionManager.createSession(currentSessionId, new String[]{readQRSessionKeys.get(0), readQRSessionKeys.get(1)}, sessionJoinCallback);
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
}
