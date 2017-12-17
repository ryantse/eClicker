package edu.ucsb.cs.cs184.eclicker;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SessionMultipleChoiceActivity extends SessionQuestion {
    Integer questionId;
    ArrayList<String> choices;
    Boolean scramble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_multiple_choice);

        Toolbar toolbar = findViewById(R.id.session_multiple_choice_toolbar);
        setSupportActionBar(toolbar);

        Bundle questionData = getIntent().getBundleExtra("questionData");

        // Retrieve the question ID
        questionId = questionData.getInt("id");

        // Retrieve the question title.
        TextView questionTitle = findViewById(R.id.session_multiple_choice_question);
        questionTitle.setText(questionData.getString("title"));

        // Retrieve whether the answer choices should be scrambled.
        scramble = questionData.getBoolean("scramble");

        // Retrieve the choices for this multiple choice question.
        choices = (ArrayList<String>) questionData.getSerializable("choices");

        if (scramble) {
            // Our seed for the random number generator is question ID + device token + session ID.
            int randomSeed = questionId;
            randomSeed += (DeviceManager.getDeviceManager().getDeviceToken() + SessionManager.getSessionManager().getSessionId()).hashCode();
            Random random = new Random(randomSeed);
            Collections.shuffle(choices, random);
        }

        AnswerAdapter answerAdapter = new AnswerAdapter(choices);

        RecyclerView recyclerView = findViewById(R.id.session_multiple_choice_answers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(answerAdapter);
    }

    private class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.ViewHolder> {
        private ArrayList<String> choices;

        public AnswerAdapter(ArrayList<String> choices) {
            this.choices = choices;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_answer, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.answerText.setText(choices.get(position));
            holder.answerNumber.setText(SessionMultipleChoiceActivity.this.getResources().getString(R.string.choice_number, position + 1));
        }

        @Override
        public int getItemCount() {
            return choices.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView answerText;
            final TextView answerNumber;
            final ProgressBar answerSubmitProgress;

            ViewHolder(View itemView) {
                super(itemView);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Since we are trying to submit the answer, this ViewHolder cannot be recycled till after we complete this operation.
                        ViewHolder.this.setIsRecyclable(false);

                        answerSubmitProgress.setVisibility(View.VISIBLE);
                        answerNumber.setVisibility(View.INVISIBLE);

                        SessionManager.Helpers.SendAnswerCallback sendAnswerCallback = new SessionManager.Helpers.SendAnswerCallback() {
                            @Override
                            public void onSendAnswerSuccess() {
                                answerSubmitProgress.setVisibility(View.INVISIBLE);
                                answerNumber.setVisibility(View.VISIBLE);

                                Toast.makeText(SessionMultipleChoiceActivity.this.getApplicationContext(), R.string.session_send_answer_result_success, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onSendAnswerFailure(String failureReason) {
                                answerSubmitProgress.setVisibility(View.INVISIBLE);
                                answerNumber.setVisibility(View.VISIBLE);

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

                                Toast.makeText(SessionMultipleChoiceActivity.this.getApplicationContext(), displayMessageId, Toast.LENGTH_LONG).show();
                            }
                        };

                        try {
                            JSONObject answerData = new JSONObject();
                            answerData.put("answer", answerText.getText());
                            SessionManager.Helpers.sendAnswer(SessionMultipleChoiceActivity.this.questionId, answerData, sendAnswerCallback);
                        } catch (JSONException exception) {
                            sendAnswerCallback.onSendAnswerFailure("INTERNAL_ERROR");
                        }
                    }
                });

                answerText = itemView.findViewById(R.id.answer_text);
                answerNumber = itemView.findViewById(R.id.answer_number);

                answerSubmitProgress = itemView.findViewById(R.id.answer_submit_progress);
                answerSubmitProgress.setVisibility(View.INVISIBLE);
            }
        }
    }
}
