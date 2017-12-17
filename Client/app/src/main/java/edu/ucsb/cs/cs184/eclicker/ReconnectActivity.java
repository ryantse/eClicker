package edu.ucsb.cs.cs184.eclicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

public class ReconnectActivity extends AppCompatActivity {
    static final Integer retryCountMax = 3;
    static final Integer retryDelay = 3;
    Integer retryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reconnect);

        Toolbar toolbar = findViewById(R.id.reconnect_toolbar);
        setSupportActionBar(toolbar);

        retryCount = 0;
        new Thread(new ReconnectRunnable()).start();

        Button retryButton = findViewById(R.id.reconnect_retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryCount = 0;
                new Thread(new ReconnectRunnable()).start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reconnect, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_reconnect_device_id).setTitle(getResources().getString(R.string.menu_device_id, DeviceManager.getDeviceManager().getDeviceId()));
        return super.onPrepareOptionsMenu(menu);
    }

    private class ReconnectRunnable implements Runnable {
        @Override
        public void run() {
            final Button retryButton = ReconnectActivity.this.findViewById(R.id.reconnect_retry);
            final TextView textView = ReconnectActivity.this.findViewById(R.id.reconnect_status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(R.string.application_status);
                    retryButton.setEnabled(false);
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
                        ReconnectRunnable.this.run();
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
                    final SessionManager.SessionJoinCallback sessionJoinCallback = new SessionManager.SessionJoinCallback() {
                        @Override
                        public void onSessionJoinSuccess() {
                            connectionManager.removeMessageListener(messageListenerReference);

                            // Since we successfully joined a session, then we should move to the waiting screen.
                            Intent intent = new Intent(ReconnectActivity.this.getApplicationContext(), SessionWaitingActivity.class);
                            ReconnectActivity.this.startActivity(intent);
                            ReconnectActivity.this.finish();
                        }

                        @Override
                        public void onSessionJoinFailure(String failureReason) {
                            connectionManager.removeMessageListener(messageListenerReference);
                            SessionManager.getSessionManager().exitSession();

                            Intent intent = new Intent(ReconnectActivity.this.getApplicationContext(), QRScanActivity.class);
                            ReconnectActivity.this.startActivity(intent);
                            ReconnectActivity.this.finish();
                        }
                    };

                    DeviceManager.DeviceJoinCallback deviceJoinCallback = new DeviceManager.DeviceJoinCallback() {
                        @Override
                        public void onDeviceJoinSuccess() {
                            // Try to do a session join.
                            SessionManager.Initialize(ReconnectActivity.this.getApplicationContext());
                            SessionManager sessionManager = SessionManager.getSessionManager();
                            sessionManager.resumeSession(sessionJoinCallback);
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
            };

            if (connectionManager.isConnected()) {
                onNetworkConnected.handleMessage(null);
            } else {
                connectionManager.onMessageOnce(ConnectionManager.MessageType.GENERAL_NETWORK_CONNECTED, onNetworkConnected);
            }
        }
    }
}
