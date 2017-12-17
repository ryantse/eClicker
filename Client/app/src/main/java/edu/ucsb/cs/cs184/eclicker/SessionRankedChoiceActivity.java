package edu.ucsb.cs.cs184.eclicker;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SessionRankedChoiceActivity extends SessionQuestion {
    private Integer questionId;
    private ArrayList<String> choicesList;
    private ArrayList<Pair<Integer, String>> choices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_ranked_choice);

        Toolbar toolbar = findViewById(R.id.session_ranked_choice_toolbar);
        setSupportActionBar(toolbar);

        Bundle questionData = getIntent().getBundleExtra("questionData");

        // Retrieve the question ID
        questionId = questionData.getInt("id");

        // Retrieve the question title.
        TextView questionTitle = findViewById(R.id.session_ranked_choice_question);
        questionTitle.setText(questionData.getString("title"));

        // Retrieve the choices for this multiple choice question.
        choicesList = (ArrayList<String>) questionData.getSerializable("choices");

        choices = new ArrayList<>();
        for (int i = 0; i < choicesList.size(); ++i) {
            choices.add(new Pair<>(i, choicesList.get(i)));
        }

        final DragListView dragListView = findViewById(R.id.session_ranked_choice_answers);
        dragListView.setLayoutManager(new LinearLayoutManager(this));
        dragListView.setCanDragHorizontally(false);
        final ChoiceAdapter choiceAdapter = new ChoiceAdapter(choices);
        dragListView.setAdapter(choiceAdapter, true);

        Button buttonSubmit = findViewById(R.id.session_ranked_choice_submit);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionManager.Helpers.SendAnswerCallback sendAnswerCallback = new SessionManager.Helpers.SendAnswerCallback() {
                    @Override
                    public void onSendAnswerSuccess() {
                        Toast.makeText(SessionRankedChoiceActivity.this.getApplicationContext(), R.string.session_send_answer_result_success, Toast.LENGTH_LONG).show();
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

                        Toast.makeText(SessionRankedChoiceActivity.this.getApplicationContext(), displayMessageId, Toast.LENGTH_LONG).show();
                    }
                };

                try {
                    // Create the JSON array of answer choices.
                    JSONArray answerOrder = new JSONArray();
                    for(int i = 0; i < choiceAdapter.getItemCount(); ++i) {
                        answerOrder.put(choicesList.get((int)choiceAdapter.getUniqueItemId(i)));
                    }

                    JSONObject answerData = new JSONObject();
                    answerData.put("answer", answerOrder);
                    SessionManager.Helpers.sendAnswer(SessionRankedChoiceActivity.this.questionId, answerData, sendAnswerCallback);
                } catch (JSONException exception) {
                    sendAnswerCallback.onSendAnswerFailure("INTERNAL_ERROR");
                }
            }
        });
    }

    private class ChoiceAdapter extends DragItemAdapter<Pair<Integer, String>, ChoiceAdapter.ViewHolder> {
        ChoiceAdapter(ArrayList<Pair<Integer, String>> list) {
            setItemList(list);
        }

        @Override
        public long getUniqueItemId(int position) {
            return mItemList.get(position).first;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_choice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChoiceAdapter.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            holder.choiceText.setText(mItemList.get(position).second);
            holder.itemView.setTag(mItemList.get(position));
        }

        class ViewHolder extends DragItemAdapter.ViewHolder {
            final TextView choiceText;

            ViewHolder(final View itemView) {
                super(itemView, R.id.choice_handle, false);
                choiceText = itemView.findViewById(R.id.choice_text);
            }
        }
    }
}
