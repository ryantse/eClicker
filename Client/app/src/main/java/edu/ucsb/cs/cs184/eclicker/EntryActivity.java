package edu.ucsb.cs.cs184.eclicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

public class EntryActivity extends AppCompatActivity {
    static final Integer retryCountMax = 3;
    static final Integer retryDelay = 3;
    Integer retryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        retryCount = 0;
        new Thread(new InitializeRunnable()).start();

        Button retryButton = findViewById(R.id.entry_retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryCount = 0;
                new Thread(new InitializeRunnable()).start();
            }
        });
    }

    private class InitializeRunnable implements Runnable {
        @Override
        public void run() {
            final Button retryButton = EntryActivity.this.findViewById(R.id.entry_retry);
            final TextView textView = EntryActivity.this.findViewById(R.id.entry_status);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    retryButton.setEnabled(false);
                    textView.setText(R.string.application_status);
                }
            });

            final ConnectionManager.MessageListenerReference messageListenerReference = ConnectionManager.onNetworkErrorOnce(new ConnectionManager.MessageListener() {
                @Override
                public void handleMessage(JSONObject messageData) {
                    if (++retryCount <= retryCountMax) {
                        try {
                            for (int i = retryDelay; i > 0; --i) {
                                final int seconds = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textView.setText(getResources().getQuantityString(R.plurals.application_status_connection_failed_retry, seconds, seconds));
                                    }
                                });
                                Thread.sleep(1000);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(R.string.application_status);
                                }
                            });
                            Thread.sleep(100);
                        } catch (InterruptedException exception) {
                            System.err.println(exception);
                        }
                        InitializeRunnable.this.run();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(R.string.application_status_connection_failed);

                                retryButton.setVisibility(View.VISIBLE);
                                retryButton.setEnabled(true);
                            }
                        });
                    }
                }
            });

            final ConnectionManager connectionManager = ConnectionManager.getInstance().get();

            ConnectionManager.MessageListener onNetworkConnected = new ConnectionManager.MessageListener() {
                @Override
                public void handleMessage(JSONObject messageData) {
                    DeviceManager.Initialize(getApplicationContext());
                    DeviceManager deviceManager = DeviceManager.getDeviceManager();
                    deviceManager.deviceJoin(new DeviceManager.DeviceJoinCallback() {
                        @Override
                        public void onDeviceJoinSuccess() {
                            // Try to do a session join.
                            SessionManager.Initialize(EntryActivity.this.getApplicationContext());
                            SessionManager sessionManager = SessionManager.getSessionManager();
                            sessionManager.resumeSession(new SessionManager.SessionJoinCallback() {
                                @Override
                                public void onSessionJoinSuccess() {
                                    connectionManager.removeMessageListener(messageListenerReference);

                                    // Since we successfully joined a session, then we should move to the waiting screen.
                                    Intent intent = new Intent(EntryActivity.this.getApplicationContext(), SessionWaitingActivity.class);
                                    EntryActivity.this.startActivity(intent);
                                    EntryActivity.this.finish();
                                }

                                @Override
                                public void onSessionJoinFailure(String failureReason) {
                                    connectionManager.removeMessageListener(messageListenerReference);
                                    SessionManager.getSessionManager().exitSession();

                                    Intent intent = new Intent(EntryActivity.this.getApplicationContext(), QRScanActivity.class);
                                    EntryActivity.this.startActivity(intent);
                                    EntryActivity.this.finish();
                                }
                            });
                        }

                        @Override
                        public void onDeviceJoinFailure(String failureReason) {
                            if (failureReason.equals("DEVICE_TOKEN_INVALID")) {
                                DeviceManager deviceManager = DeviceManager.getDeviceManager();
                                deviceManager.reregisterDevice();
                                deviceManager.deviceJoin(this);
                            }
                        }
                    });
                }
            };

            // There is a chance that we can be connected before registering the handler for GENERAL_NETWORK_CONNECTED.
            if (connectionManager.isConnected()) {
                onNetworkConnected.handleMessage(null);
            } else {
                connectionManager.onMessageOnce(ConnectionManager.MessageType.GENERAL_NETWORK_CONNECTED, onNetworkConnected);
            }
        }
    }
}
