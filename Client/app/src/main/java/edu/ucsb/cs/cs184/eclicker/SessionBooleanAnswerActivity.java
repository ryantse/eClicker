package edu.ucsb.cs.cs184.eclicker;

import android.os.Bundle;

import java.util.ArrayList;

/**
 * Created by Ryan on 12/14/17.
 */

public class SessionBooleanAnswerActivity extends SessionMultipleChoiceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle questionData = getIntent().getBundleExtra("questionData");

        ArrayList<String> choices = new ArrayList<>();
        choices.add("True");
        choices.add("False");
        questionData.putSerializable("choices", choices);
        questionData.putSerializable("scramble", false);

        getIntent().removeExtra("questionData");
        getIntent().putExtra("questionData", questionData);

        super.onCreate(savedInstanceState);
    }
}
