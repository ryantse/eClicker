package edu.ucsb.cs.cs184.eclicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Created by Ryan on 12/12/17.
 */

abstract public class SessionQuestion extends AppCompatActivity implements SessionManager.SessionEventCallback {
    SessionManager.SessionListenerReference sessionListenerReference;
    ConnectionManager.MessageListenerReference messageListenerReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register this activity to receive events about new questions or the end of a session.
        sessionListenerReference = SessionManager.getSessionManager().addSessionListener(this);

        // If we lose the connection to the server. We should move to the reconnecting screen.
        messageListenerReference = ConnectionManager.getInstance().get().onMessageOnce(ConnectionManager.MessageType.GENERAL_NETWORK_DISCONNECTED, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                Intent intent = new Intent(SessionQuestion.this.getApplicationContext(), ReconnectActivity.class);
                SessionQuestion.this.startActivity(intent);
                SessionQuestion.this.finish();
            }
        });

        invalidateOptionsMenu();
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
                        Toast.makeText(SessionQuestion.this.getApplicationContext(), R.string.session_ask_question_result_success, Toast.LENGTH_LONG).show();
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

                            default:
                                displayMessageId = R.string.session_ask_question_result_internal_error;
                                break;
                        }

                        Toast.makeText(SessionQuestion.this.getApplicationContext(), displayMessageId, Toast.LENGTH_LONG).show();
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

    @Override
    public void onSessionQuestionBegin(JSONObject messageData) {
        // Since we are moving to a new activity, this activity is no longer interested.
        ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);
        SessionManager.getSessionManager().removeSessionListener(sessionListenerReference);

        Intent intent = SessionManager.Helpers.parseQuestionMessage(this, messageData);
        if (intent == null) {
            // If we fail to parse the question begin message, go back to the session waiting activity.
            intent = new Intent(this.getApplicationContext(), SessionWaitingActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onSessionQuestionEnd() {
        // Since we are moving to a new activity, this activity is no longer interested.
        ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);
        SessionManager.getSessionManager().removeSessionListener(sessionListenerReference);

        Intent intent = new Intent(this.getApplicationContext(), SessionWaitingActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSessionTerminate() {
        // Since we are moving to a new activity, this activity is no longer interested.
        ConnectionManager.getInstance().get().removeMessageListener(messageListenerReference);
        SessionManager.getSessionManager().removeSessionListener(sessionListenerReference);

        Intent intent = new Intent(this.getApplicationContext(), QRScanActivity.class);
        startActivity(intent);
        finish();
    }
}