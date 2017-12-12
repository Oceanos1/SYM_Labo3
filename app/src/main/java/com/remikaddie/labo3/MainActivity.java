package com.remikaddie.labo3;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openActivity(View view) {
        // Finding the corresponding activity
        Class newActivity = null;
        switch (view.getId()) {
            case R.id.nfcButton:
                newActivity = NfcActivity.class;
                break;
            case R.id.barCodeButton:
                newActivity = BarcodeActivity.class;
                break;
            case R.id.iBeaconButton:
                newActivity = IBeaconActivity.class;
                break;
            case R.id.captorButton:
                newActivity = CompassActivity.class;
                break;
        }

        if(newActivity != null) {
            // Starting the activity
            Intent intent = new Intent(this, newActivity);
            startActivity(intent);
        }
    }
}
