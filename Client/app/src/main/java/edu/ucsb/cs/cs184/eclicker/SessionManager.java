package edu.ucsb.cs.cs184.eclicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import cz.msebera.android.httpclient.Header;


public class SessionManager {
    public static final String SESSION_ID = "session_id";
    public static final String SESSION_TOKEN = "session_token";
    private static SessionManager sessionManager = null;
    private final SharedPreferences sharedPreferences;
    private final ArrayList<SessionListenerReference> sessionListeners = new ArrayList<>();
    private String activeSessionId = null;
    private String activeSessionToken = null;
    private WeakReference<ConnectionManager> currentConnectionManager;
    private OnSessionQuestionBegin onSessionQuestionBegin;
    private OnSessionQuestionEnd onSessionQuestionEnd;
    private OnSessionTerminate onSessionTerminate;

    private SessionManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;

        ConnectionManager.onNetworkError(new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                // Since this current network has failed, we no longer have an active session.
                SessionManager.this.activeSessionId = null;
                SessionManager.this.activeSessionToken = null;
            }
        });

        currentConnectionManager = ConnectionManager.getInstance();
        this.registerSessionHandlers();
    }

    public static void Initialize(Context context) {
        if (sessionManager == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SessionManager.class.getName(), Context.MODE_PRIVATE);
            sessionManager = new SessionManager(sharedPreferences);
        }
    }

    private void registerSessionHandlers() {
        ConnectionManager connectionManager = currentConnectionManager.get();
        onSessionQuestionBegin = new OnSessionQuestionBegin().register(connectionManager);
        onSessionQuestionEnd = new OnSessionQuestionEnd().register(connectionManager);
        onSessionTerminate = new OnSessionTerminate().register(connectionManager);
    }

    private void unregisterSessionHandlers() {
        onSessionQuestionBegin.unregister();
        onSessionQuestionEnd.unregister();
        onSessionTerminate.unregister();
    }

    public static SessionManager getSessionManager() {
        return sessionManager;
    }

    public String getSessionId() {
        return this.activeSessionId;
    }

    public String getSessionToken() {
        return this.activeSessionToken;
    }

    public SessionListenerReference addSessionListener(SessionEventCallback sessionEventCallback) {
        synchronized (SessionManager.class) {
            SessionListenerReference sessionListenerReference = new SessionListenerReference(sessionEventCallback);
            sessionListeners.add(sessionListenerReference);
            return sessionListenerReference;
        }
    }

    public void removeSessionListener(SessionListenerReference sessionListenerReference) {
        synchronized (SessionManager.class) {
            sessionListeners.remove(sessionListenerReference);
        }
    }

    public void exitSession() {
        this.activeSessionId = null;
        this.activeSessionToken = null;

        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.remove(SESSION_ID);
        editor.remove(SESSION_TOKEN);
        editor.commit();
    }

    private boolean hasResumableSession() {
        return (this.sharedPreferences.getString(SESSION_ID, null) != null && this.sharedPreferences.getString(SESSION_TOKEN, null) != null);
    }

    public void resumeSession(final SessionJoinCallback callback) {
        if (hasResumableSession()) {
            createSession(this.sharedPreferences.getString(SESSION_ID, null), this.sharedPreferences.getString(SESSION_TOKEN, null), callback);
        } else {
            callback.onSessionJoinFailure("NO_RESUMABLE_SESSION");
        }
    }

    private void prepareCreateSession(final ConnectionManager connectionManager, final SessionJoinCallback callback) {
        connectionManager.onMessageOnce(ConnectionManager.MessageType.SESSION_JOIN, new ConnectionManager.MessageListener() {
            @Override
            public void handleMessage(JSONObject messageData) {
                try {
                    switch (messageData.getString("status")) {
                        case "OK":
                            JSONObject sessionData = messageData.getJSONObject("statusExtended");
                            String sessionId = sessionData.getString("sessionId");
                            String sessionToken = sessionData.getString("sessionToken");

                            SessionManager.this.activeSessionId = sessionId;
                            SessionManager.this.activeSessionToken = sessionToken;

                            // Save the current session ID and token to shared preferences.
                            SharedPreferences.Editor editor = SessionManager.this.sharedPreferences.edit();
                            editor.putString(SESSION_ID, sessionId);
                            editor.putString(SESSION_TOKEN, sessionToken);
                            editor.commit();

                            callback.onSessionJoinSuccess();
                            break;

                        case "ERROR":
                            callback.onSessionJoinFailure(messageData.getJSONObject("statusExtended").getString("errorReason"));
                            break;
                    }
                } catch (JSONException exception) {
                    System.err.println("Malformed JSON object received.");
                }
            }
        });
    }

    public void createSession(final String sessionId, String[] keys, final SessionJoinCallback callback) {
        // A session cannot be created if there is an active session.
        if(this.isSessionActive()) {
            return;
        }

        ConnectionManager connectionManager = ConnectionManager.getInstance().get();
        if(currentConnectionManager.get() != connectionManager) {
            currentConnectionManager = ConnectionManager.getInstance();
            registerSessionHandlers();
        }
        prepareCreateSession(connectionManager, callback);

        try {
            JSONObject messageData = new JSONObject()
                    .put("authenticationType", "sessionKey")
                    .put("sessionId", sessionId)
                    .put("sessionKeys", new JSONArray(keys));
            connectionManager.sendMessage(ConnectionManager.MessageType.SESSION_JOIN, messageData);
        } catch (JSONException exception) {
            System.err.println("Failed to create message data for SESSION_JOIN.");
        }
    }

    public void createSession(final String sessionId, String sessionToken, final SessionJoinCallback callback) {
        // A session cannot be created if there is an active session.
        if(this.isSessionActive()) {
            return;
        }

        ConnectionManager connectionManager = ConnectionManager.getInstance().get();
        if(currentConnectionManager.get() != connectionManager) {
            currentConnectionManager = ConnectionManager.getInstance();
            registerSessionHandlers();
        }
        prepareCreateSession(connectionManager, callback);

        try {
            JSONObject messageData = new JSONObject()
                    .put("authenticationType", "sessionToken")
                    .put("sessionId", sessionId)
                    .put("sessionToken", sessionToken);
            connectionManager.sendMessage(ConnectionManager.MessageType.SESSION_JOIN, messageData);
        } catch (JSONException exception) {
            System.err.println("Failed to create message data for SESSION_JOIN.");
        }
    }

    public boolean isSessionActive() {
        return (this.getSessionId() != null && this.getSessionToken() != null);
    }

    enum QuestionType {MultipleChoice, RankedChoice, BooleanAnswer, ShortAnswer}

    public interface SessionJoinCallback {
        void onSessionJoinSuccess();

        void onSessionJoinFailure(String failureReason);
    }

    public interface SessionEventCallback {
        void onSessionQuestionBegin(JSONObject messageData);

        void onSessionQuestionEnd();

        void onSessionTerminate();
    }

    static class Helpers {
        static void confirmSessionExit(final Activity activity) {
            AlertDialog confirmDialog = new AlertDialog.Builder(activity)
                    .setCancelable(true)
                    .setTitle(R.string.session_exit_confirm_title)
                    .setIcon(R.drawable.ic_session_exit)
                    .setMessage(R.string.session_exit_confirm)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SessionManager.getSessionManager().exitSession();

                            // Move us back to the QR Scan Activity if we are exiting the current session.
                            Intent intent = new Intent(activity.getApplicationContext(), QRScanActivity.class);
                            activity.startActivity(intent);
                            activity.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            confirmDialog.show();
        }

        static void sendInstructorQuestion(final Activity activity, final AskQuestionCallback callback) {
            LayoutInflater layoutInflater = LayoutInflater.from(activity);
            View askQuestionView = layoutInflater.inflate(R.layout.dialog_ask_question, null);
            final EditText askQuestionInput = askQuestionView.findViewById(R.id.ask_question_input);

            AlertDialog askQuestionDialog = new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(R.string.session_ask_question_title)
                    .setIcon(R.drawable.ic_ask_question)
                    .setView(askQuestionView)
                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Check if the input is empty.
                            if(askQuestionInput.getText().length() == 0) {
                                return;
                            }

                            // Prepare the request parameters to send to the server.
                            RequestParams requestParams = new RequestParams();
                            requestParams.put("token", sessionManager.getSessionToken());
                            requestParams.put("question", askQuestionInput.getText());

                            AsyncHttpClient client = new AsyncHttpClient();
                            client.post("https://" + ConnectionManager.serverHost + "/mobile/session/ask-question", requestParams, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                                    try {
                                        switch(jsonObject.getString("status")) {
                                            case "OK":
                                                callback.onAskQuestionSuccess();
                                                break;

                                            case "ERROR":
                                                callback.onAskQuestionFailure(jsonObject.getString("statusExtended"));
                                                break;
                                        }
                                    } catch (JSONException exception) {
                                        callback.onAskQuestionFailure("INTERNAL_ERROR");
                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                                    callback.onAskQuestionFailure("INTERNAL_ERROR");
                                }
                            });
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            askQuestionDialog.show();
        }

        static Intent parseQuestionMessage(Context context, JSONObject messageData) {
            try {
                Intent returnIntent = null;
                Integer questionId = messageData.getInt("questionId");
                String questionTitle = messageData.getString("questionTitle");

                /* We pass the data for a question as a Bundle. The minimum bundle has the question ID
                 * and the question title which are the basics that every question will ahve.
                 */
                Bundle bundle = new Bundle();
                bundle.putInt("id", questionId);
                bundle.putString("title", questionTitle);

                switch (QuestionType.valueOf(messageData.getString("questionType"))) {
                    case MultipleChoice: {
                        // We try to unpack the JSON objects first before we do any more expensive operations.
                        JSONObject questionData = messageData.getJSONObject("questionData");
                        JSONArray questionDataChoices = questionData.getJSONArray("choices");

                        Boolean questionScrambleOptions = questionData.getBoolean("scramble");

                        // Load the answer choices into an ArrayList.
                        ArrayList<String> questionChoices = new ArrayList<>();
                        for (int i = 0; i < questionDataChoices.length(); ++i) {
                            questionChoices.add(questionDataChoices.getString(i));
                        }

                        // Put the question data into the bundle.
                        bundle.putBoolean("scramble", questionScrambleOptions);
                        bundle.putSerializable("choices", questionChoices);

                        // Prepare the intent.
                        returnIntent = new Intent(context.getApplicationContext(), SessionMultipleChoiceActivity.class);
                        returnIntent.putExtra("questionData", bundle);
                    }
                    break;

                    case RankedChoice: {
                        // We try to unpack the JSON objects first before we do any more expensive operations.
                        JSONObject questionData = messageData.getJSONObject("questionData");
                        JSONArray questionDataChoices = questionData.getJSONArray("choices");

                        // Load the answer choices into an ArrayList.
                        ArrayList<String> questionChoices = new ArrayList<>();
                        for (int i = 0; i < questionDataChoices.length(); ++i) {
                            questionChoices.add(questionDataChoices.getString(i));
                        }

                        bundle.putSerializable("choices", questionChoices);

                        returnIntent = new Intent(context.getApplicationContext(), SessionRankedChoiceActivity.class);
                        returnIntent.putExtra("questionData", bundle);
                    }
                    break;

                    case ShortAnswer: {
                        // There are no additional parameters associated with a Short Answer question.
                        returnIntent = new Intent(context.getApplicationContext(), SessionShortAnswerActivity.class);
                        returnIntent.putExtra("questionData", bundle);
                    }
                    break;

                    case BooleanAnswer: {
                        // There are no additional parameters associated with a Short Answer question.
                        returnIntent = new Intent(context.getApplicationContext(), SessionBooleanAnswerActivity.class);
                        returnIntent.putExtra("questionData", bundle);
                    }
                    break;
                }

                // We pass this intent back to the calling function. This allows them to control when to move to the next question.
                return returnIntent;
            } catch (JSONException exception) {
                System.err.println("Malformed JSON object received.");
            }

            return null;
        }

        static void sendAnswer(Integer questionId, JSONObject answerData, final SendAnswerCallback callback) {
            RequestParams requestParams = new RequestParams();
            requestParams.put("token", sessionManager.getSessionToken());
            requestParams.put("questionId", questionId);
            requestParams.put("answerData", answerData.toString());

            AsyncHttpClient client = new AsyncHttpClient();
            client.post("https://" + ConnectionManager.serverHost + "/mobile/session/record-answer", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                    try {
                        switch(jsonObject.getString("status")) {
                            case "OK":
                                callback.onSendAnswerSuccess();
                                break;

                            case "ERROR":
                                callback.onSendAnswerFailure(jsonObject.getString("statusExtended"));
                                break;
                        }
                    } catch (JSONException exception) {
                        callback.onSendAnswerFailure("INTERNAL_ERROR");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                    callback.onSendAnswerFailure("INTERNAL_ERROR");
                }
            });
        }

        interface SendAnswerCallback {
            void onSendAnswerSuccess();
            void onSendAnswerFailure(String failureReason);
        }

        interface AskQuestionCallback {
            void onAskQuestionSuccess();
            void onAskQuestionFailure(String failureReason);
        }
    }

    class SessionListenerReference {
        final SessionEventCallback sessionEventCallback;

        SessionListenerReference(SessionEventCallback sessionEventCallback) {
            this.sessionEventCallback = sessionEventCallback;
        }
    }

    abstract class OnSessionMessageListener<T> implements ConnectionManager.MessageListener{
        private ConnectionManager.MessageListenerReference messageListenerReference;
        private WeakReference<ConnectionManager> connectionManager;
        protected ConnectionManager.MessageType messageType;

        @Override
        abstract public void handleMessage(JSONObject messageData);

        public T register(ConnectionManager connectionManager) {
            this.messageListenerReference = connectionManager.onMessage(messageType, this);
            this.connectionManager = new WeakReference<>(connectionManager);
            return (T)this;
        }

        public void unregister() {
            ConnectionManager connectionManager = this.connectionManager.get();
            if(connectionManager == null) return;
            connectionManager.removeMessageListener(this.messageListenerReference);
        }
    }

    class OnSessionQuestionBegin extends OnSessionMessageListener<OnSessionQuestionBegin> {
        OnSessionQuestionBegin() {
            messageType = ConnectionManager.MessageType.QUESTION_BEGIN;
        }

        @Override
        public void handleMessage(JSONObject messageData) {
            for (SessionListenerReference sessionListenerReference : SessionManager.this.sessionListeners) {
                sessionListenerReference.sessionEventCallback.onSessionQuestionBegin(messageData);
            }
        }
    }

    class OnSessionQuestionEnd extends OnSessionMessageListener<OnSessionQuestionEnd> {
        OnSessionQuestionEnd() {
            messageType = ConnectionManager.MessageType.QUESTION_END;
        }

        @Override
        public void handleMessage(JSONObject messageData) {
            for (SessionListenerReference sessionListenerReference : SessionManager.this.sessionListeners) {
                sessionListenerReference.sessionEventCallback.onSessionQuestionEnd();
            }
        }
    }

    class OnSessionTerminate extends OnSessionMessageListener<OnSessionTerminate> {
        OnSessionTerminate() {
            messageType = ConnectionManager.MessageType.SESSION_TERMINATE;
        }

        @Override
        public void handleMessage(JSONObject messageData) {
            SessionManager.this.exitSession();
            for (SessionListenerReference sessionListenerReference : SessionManager.this.sessionListeners) {
                sessionListenerReference.sessionEventCallback.onSessionTerminate();
            }
        }
    }
}
