package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Dong on 12/9/17.
 */

public class WaitingActivity extends Activity {

    public ProgressBar loadingCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waiting_layout);

        String className = getIntent().getStringExtra("CLASS_TO_DISPLAY");
        TextView titleDisplay = findViewById(R.id.currentClassText);
        titleDisplay.setText(className);

        loadingCircle = findViewById(R.id.progressBar);
        loadingCircle.setVisibility(View.VISIBLE);

    }
}
