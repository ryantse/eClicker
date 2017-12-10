package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by Dong on 12/9/17.
 */

public class FreeResponse extends Activity {

    public String outputString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.free_response);

        String title = getIntent().getStringExtra("QUESTION_TO_DISPLAY");
        TextView titleDisplay = findViewById(R.id.currentQuestionText);
        titleDisplay.setText(title);

        FloatingActionButton sendFAB = findViewById(R.id.sendAnswerFAB);
        sendFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText response = findViewById(R.id.editText);
                outputString = response.getText().toString();
                //send outputString here
            }
        });
    }
}
