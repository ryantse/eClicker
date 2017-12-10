package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Created by Dong on 12/9/17.
 */

public class BooleanQuestion extends Activity {

    public boolean outputBool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.true_false);

        String title = getIntent().getStringExtra("QUESTION_TO_DISPLAY");
        TextView titleDisplay = findViewById(R.id.currentQuestionText);
        titleDisplay.setText(title);

        FloatingActionButton sendFAB = findViewById(R.id.sendAnswerFAB);
        sendFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addListenerOnButton();
                //send outputBool
            }
        });
    }

    public void addListenerOnButton() {

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        RadioButton trueBut = findViewById(R.id.answerTruebutton);
        RadioButton falseBut = findViewById(R.id.answerFalsebutton);

        int trueID = trueBut.getId();
        int falseID = falseBut.getId();

        int selectedID = radioGroup.getCheckedRadioButtonId();

        if (selectedID == trueID) {
            outputBool = true;
        } else if (selectedID == falseID) {
            outputBool = false;
        }

    }

}
