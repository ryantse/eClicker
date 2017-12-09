package edu.ucsb.cs.cs184.elicker.eclicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.widget.TextView;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.util.ArrayList;

/**
 * Created by Dong on 12/8/17.
 */

public class QRscanActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    private QRCodeReaderView qrCodeReaderView;
    private TextView myQRoutput;

    private ArrayList<String> ReadQR = new ArrayList<String>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        qrCodeReaderView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        myQRoutput = findViewById(R.id.instructionText);

        qrCodeReaderView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true);

        // Use this function to change the autofocus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(2000L);

        // Use this function to enable/disable Torch
        qrCodeReaderView.setTorchEnabled(true);

        // Use this function to set front camera preview
        //qrCodeReaderView.setFrontCamera();

        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera();
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        //myQRoutput.setText(text);
        //System.out.println(text);

        if(ReadQR.size() == 2){
            System.out.println(ReadQR);
            //Intent intent = new Intent(this, );
            //startActivity(intent);
            ReadQR.remove(0);
        }
        ReadQR.add(text);

    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeReaderView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeReaderView.stopCamera();
    }
}
