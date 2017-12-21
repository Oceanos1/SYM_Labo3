package com.remikaddie.labo3;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

public class NfcActivity extends Activity {
    //Tries to authenticate
    private Button nfcAccessButton;
    //Choose whether to authenticate using both NFC and password or just one of these
    private Switch doubleAuth;
    //TextViews to show if NFC has been scanned and to show secure data when authenticated
    private TextView nfcStatus, secureDataView;
    //Current level of authentication. Decreases over time
    private int authenticationLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAccessButton = (Button)findViewById(R.id.barCodeButton);
        doubleAuth = (Switch)findViewById(R.id.nfc_auth_switch);
        nfcStatus = (TextView)findViewById(R.id.nfc_status);
        secureDataView = (TextView)findViewById(R.id.secure_data_view);
        authenticationLevel = 0;

        setContentView(R.layout.activity_nfc);
    }

}
