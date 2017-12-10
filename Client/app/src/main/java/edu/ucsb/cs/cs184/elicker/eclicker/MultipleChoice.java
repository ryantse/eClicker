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

public class MultipleChoice extends Activity {

    public String outputLetter;

    public String choiceOne;
    public String choiceTwo;
    public String choiceThree;
    public String choiceFour;
    public String choiceFive;

    public RadioButton aBut;
    public RadioButton bBut;
    public RadioButton cBut;
    public RadioButton dBut;
    public RadioButton eBut;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_choice);

        String title = getIntent().getStringExtra("QUESTION_TO_DISPLAY");
        TextView titleDisplay = findViewById(R.id.currentQuestionText);
        titleDisplay.setText(title);

        aBut = findViewById(R.id.answerAbutton);
        bBut = findViewById(R.id.answerBbutton);
        cBut = findViewById(R.id.answerCbutton);
        dBut = findViewById(R.id.answerDbutton);
        eBut = findViewById(R.id.answerEbutton);

        String temp1 = getIntent().getStringExtra("ANSWER_A");
        aBut.setText(temp1);
        String temp2 = getIntent().getStringExtra("ANSWER_B");
        bBut.setText(temp2);
        String temp3 = getIntent().getStringExtra("ANSWER_C");
        cBut.setText(temp3);
        String temp4 = getIntent().getStringExtra("ANSWER_D");
        dBut.setText(temp4);
        String temp5 = getIntent().getStringExtra("ANSWER_E");
        eBut.setText(temp5);

        FloatingActionButton sendFAB = findViewById(R.id.sendAnswerFAB);
        sendFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addListenerOnButton();
                //send outputLetter
            }
        });
    }

    public void addListenerOnButton() {

        RadioGroup radioGroup = findViewById(R.id.radioGroup2);

        int aID = aBut.getId();
        int bID = bBut.getId();
        int cID = cBut.getId();
        int dID = dBut.getId();
        int eID = eBut.getId();

        int selectedID = radioGroup.getCheckedRadioButtonId();

        if(selectedID == aID){
            outputLetter = "a";
        }else if(selectedID == bID){
            outputLetter = "b";
        }else if(selectedID == cID){
            outputLetter = "c";
        }else if(selectedID == dID){
            outputLetter = "d";
        }else if(selectedID == eID){
            outputLetter = "e";
        }

    }
}
