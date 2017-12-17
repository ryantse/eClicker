package edu.ucsb.cs.cs184.eclicker;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class SessionShortAnswerActivity extends SessionQuestion {
    private Integer questionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_short_answer);

        Toolbar toolbar = findViewById(R.id.session_short_answer_toolbar);
        setSupportActionBar(toolbar);

        Bundle questionData = getIntent().getBundleExtra("questionData");

        // Retrieve the question ID
        questionId = questionData.getInt("id");

        TextView questionTitle = findViewById(R.id.session_short_answer_question);
        questionTitle.setText(questionData.getString("title"));
        questionTitle.setMovementMethod(new ScrollingMovementMethod());

        invalidateOptionsMenu();

        final TextView answerText = findViewById(R.id.session_short_answer_answer);

        Button buttonSubmit = findViewById(R.id.session_short_answer_submit);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionManager.Helpers.SendAnswerCallback sendAnswerCallback = new SessionManager.Helpers.SendAnswerCallback() {
                    @Override
                    public void onSendAnswerSuccess() {
                        Toast.makeText(SessionShortAnswerActivity.this.getApplicationContext(), R.string.session_send_answer_result_success, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSendAnswerFailure(String failureReason) {
                        Integer displayMessageId;
                        switch(failureReason) {
                            case "AUTHENTICATOR_CODE_INVALID":
                                displayMessageId = R.string.session_send_answer_result_authenticator_error;
                                break;

                            case "SESSION_EXPIRED":
                                displayMessageId = R.string.session_send_answer_result_session_expired;
                                break;

                            case "QUESTION_CLOSED":
                                displayMessageId = R.string.session_send_answer_result_question_closed;
                                break;

                            default:
                                displayMessageId = R.string.session_send_answer_result_internal_error;
                                break;
                        }

                        Toast.makeText(SessionShortAnswerActivity.this.getApplicationContext(), displayMessageId, Toast.LENGTH_LONG).show();
                    }
                };

                try {
                    JSONObject answerData = new JSONObject();
                    answerData.put("answer", answerText.getText());
                    SessionManager.Helpers.sendAnswer(SessionShortAnswerActivity.this.questionId, answerData, sendAnswerCallback);
                } catch (JSONException exception) {
                    sendAnswerCallback.onSendAnswerFailure("INTERNAL_ERROR");
                }
            }
        });
    }
}
