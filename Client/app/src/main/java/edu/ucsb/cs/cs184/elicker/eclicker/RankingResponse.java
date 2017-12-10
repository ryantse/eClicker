package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Dong on 12/9/17.
 */

public class RankingResponse extends Activity {

    public StringBuilder temp;

    public ArrayList<String> holder;

    public CheckBox answerOne;
    public CheckBox answerTwo;
    public CheckBox answerThree;
    public CheckBox answerFour;
    public CheckBox answerFive;

    public TextView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ranking_response);

        temp = new StringBuilder(500);
        holder = new ArrayList<>();

        String title = getIntent().getStringExtra("QUESTION_TO_DISPLAY");
        TextView titleDisplay = findViewById(R.id.currentQuestionText);
        titleDisplay.setText(title);

        answerOne = findViewById(R.id.checkBox);
        answerTwo = findViewById(R.id.checkBox2);
        answerThree = findViewById(R.id.checkBox3);
        answerFour = findViewById(R.id.checkBox4);
        answerFive = findViewById(R.id.checkBox5);

        previewView = findViewById(R.id.previewText);

        FloatingActionButton sendFAB = findViewById(R.id.sendAnswerFAB);
        sendFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send temp here
            }
        });
    }

    public void formatString(ArrayList<String> stringArrayList){

        temp = new StringBuilder(500);

        for(int i = 0; i < stringArrayList.size(); i++){
            temp.append(stringArrayList.get(i));
        }

        previewView.setText(temp);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkBox:
                if (checked) {
                    holder.add(answerOne.getText().toString());
                    formatString(holder);
                }else{
                    holder.remove(answerOne.getText().toString());
                    formatString(holder);
                }
                break;
            case R.id.checkBox2:
                if (checked) {
                    holder.add(answerTwo.getText().toString());
                    formatString(holder);
                }else{
                    holder.remove(answerTwo.getText().toString());
                    formatString(holder);
                }
                break;
            case R.id.checkBox3:
                if (checked) {
                    holder.add(answerThree.getText().toString());
                    formatString(holder);
                }else{
                    holder.remove(answerThree.getText().toString());
                    formatString(holder);
                }
                break;
            case R.id.checkBox4:
                if (checked) {
                    holder.add(answerFour.getText().toString());
                    formatString(holder);
                }else{
                    holder.remove(answerFour.getText().toString());
                    formatString(holder);
                }
                break;
            case R.id.checkBox5:
                if (checked) {
                    holder.add(answerFive.getText().toString());
                    formatString(holder);
                }else{
                    holder.remove(answerFive.getText().toString());
                    formatString(holder);
                }
                break;
        }
    }
}
