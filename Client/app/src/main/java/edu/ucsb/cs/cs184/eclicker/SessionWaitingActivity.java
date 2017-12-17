package edu.ucsb.cs.cs184.eclicker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class SessionWaitingActivity extends AppCompatActivity {
    ConnectionManager.MessageListenerReference messageListenerReference;
    SessionManager.SessionListenerReference sessionListenerReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_waiting);

        Toolbar toolbar = findViewById(R.id.session_waiting_toolbar);
        setSupportActionBar(toolbar);

        TextView sessionIdTextView = findViewById(R.id.session_waiting_session_id);
        sessionIdTextView.setText(getResources().getString(R.string.session_id, SessionManager.getSessionManager().getSessionId()));

        ConnectionManager connectionManager = ConnectionManager.getInstance().get();
        messageListenerReference = connectionManager.onMessageOnce(ConnectionManager.MessageType.GENERAL_NETWORK_DISCONNECTED, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                // A GENERAL_NETWORK_DISCONNECTED message implies that the Connection Manager listener has been destroyed.
                SessionManager.getSessionManager().removeSessionListener(sessionListenerReference);

                Intent intent = new Intent(SessionWaitingActivity.this.getApplicationContext(), ReconnectActivity.class);
                SessionWaitingActivity.this.startActivity(intent);
                SessionWaitingActivity.this.finish();
            }
        });

        sessionListenerReference = SessionManager.getSessionManager().addSessionListener(new SessionManager.SessionEventCallback() {
            @Override
            public void onSessionQuestionBegin(JSONObject messageData) {
                Intent questionIntent = SessionManager.Helpers.parseQuestionMessage(SessionWaitingActivity.this, messageData);

                // Do nothing unless it would start a new activity.
                if (questionIntent != null) {
                    // Only remove listeners when starting the new activity.
                    SessionManager.getSessionManager().removeSessionListener(sessionListenerReference);
                    ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);

                    SessionWaitingActivity.this.startActivity(questionIntent);
                    SessionWaitingActivity.this.finish();
                }
            }

            @Override
            public void onSessionQuestionEnd() {
                // Do nothing since this message would usually just return to this activity.
            }

            @Override
            public void onSessionTerminate() {
                SessionManager.getSessionManager().removeSessionListener(sessionListenerReference);
                ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);

                Intent intent = new Intent(SessionWaitingActivity.this.getApplicationContext(), QRScanActivity.class);
                startActivity(intent);
                finish();
            }
        });

        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ping the server to let the server know that we are ready for a question (if there is one active).
        ConnectionManager.getInstance().get().sendMessage(ConnectionManager.MessageType.QUESTION_WAITING, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_session, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_session_waiting_device_id).setTitle(getResources().getString(R.string.menu_device_id, DeviceManager.getDeviceManager().getDeviceId()));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_session_waiting_ask_question:
                SessionManager.Helpers.sendInstructorQuestion(this, new SessionManager.Helpers.AskQuestionCallback() {
                    @Override
                    public void onAskQuestionSuccess() {
                        Toast.makeText(SessionWaitingActivity.this.getApplicationContext(), R.string.session_ask_question_result_success, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAskQuestionFailure(String failureReason) {
                        Integer displayMessageId = R.string.session_ask_question_result_internal_error;
                        switch(failureReason) {
                            case "AUTHENTICATOR_CODE":
                                displayMessageId = R.string.session_ask_question_result_authenticator_error;
                                break;

                            case "SESSION_EXPIRED":
                                displayMessageId = R.string.session_ask_question_result_session_expired;
                                break;

                            case "EDUCATOR_UNAVAILABLE":
                                displayMessageId = R.string.session_ask_question_result_educator_unavailable;
                                break;

                            case "INTERNAL_ERROR":
                                displayMessageId = R.string.session_ask_question_result_internal_error;
                                break;
                        }

                        Toast.makeText(SessionWaitingActivity.this.getApplicationContext(), displayMessageId, Toast.LENGTH_LONG).show();
                    }
                });
                return true;

            case R.id.menu_session_waiting_exit_session:
                SessionManager.Helpers.confirmSessionExit(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
