package com.remikaddie.labo3;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

/**
 * Code tiré de l'exemple fourni dans la donnée, soit:
 * https://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278
 */
public class NfcActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String PASSWORD = "password";
    //Tries to authenticate
    private Button nfcAccessButton;
    //Choose whether to authenticate using both NFC and password or just one of these
    private Switch doubleAuth;
    //TextViews to show if NFC has been scanned and to show secure data when authenticated
    private TextView nfcStatus, secureDataView;
    //Current level of authentication. Decreases over time
    private int authenticationLevel;

    private EditText nfcPasswordField;

    private boolean passwordCorrect;

    String scannedText;

    private NfcAdapter nfcAdapter;

    private Timer authTimer;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        nfcAccessButton = (Button)findViewById(R.id.nfc_access_button);
        doubleAuth = (Switch)findViewById(R.id.nfc_auth_switch);
        nfcStatus = (TextView)findViewById(R.id.nfc_status);
        secureDataView = (TextView)findViewById(R.id.secure_data_view);
        authenticationLevel = 0;
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        passwordCorrect = false;

        nfcPasswordField = (EditText)findViewById(R.id.nfc_password);
        nfcAccessButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(nfcPasswordField.getText().toString().equals(PASSWORD)){
                    passwordCorrect = true;
                    if(doubleAuth.isChecked()){
                        if(authTimer != null){
                            authTimer.cancel();
                            authTimer = null;
                        }
                        secureDataView.setText("First authentification passed. You can now scan the NFC tag to gain access to the secure data");
                    }else{
                        authenticate();
                    }
                }else{
                    passwordCorrect = false;
                    if(authTimer != null){
                        authTimer.cancel();
                        authTimer = null;
                    }
                    authenticationLevel = 0;
                    secureDataView.setText("Incorrect password! Auth level set to 0");
                }

            }
        });


        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            return;
        }
        if(!nfcAdapter.isEnabled()){
            Toast.makeText(this, "NFC is not enabled.", Toast.LENGTH_LONG).show();
        }
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    // called in onResume()
    private void setupForegroundDispatch(final Activity activity, NfcAdapter nfcAdapter) {
        if(nfcAdapter == null)
            return;
        final Intent intent = new Intent(this.getApplicationContext(),
                this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent =
                PendingIntent.getActivity(this.getApplicationContext(), 0, intent, 0);
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e("NFC", "MalformedMimeTypeException", e);
        }

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
    }

    // called in onPause()
    private void stopForegroundDispatch(final Activity activity, NfcAdapter nfcAdapter) {
        if(nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                nfcStatus.setText("NFC tag scanned! : "+result);
                scannedText = result;
                if(doubleAuth.isChecked()){
                    if(!passwordCorrect){
                        secureDataView.setText("Please enter password before scanning NFC tag");
                        return;
                    }
                }
                authenticate();
            }
        }
    }

    private void authenticate() {
        authenticationLevel = 10;
        if(authTimer != null){
            authTimer.cancel();
        }
        NfcActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                secureDataView.setText("You can access secure data with an authentification level of "
                        + authenticationLevel + ". Scan NFC tag to set to maximum.");
            }
        });

        authTimer = new Timer();
        //Reduce auth level by one every 5 seconds
        authTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                authenticationLevel--;
                NfcActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        secureDataView.setText("You can access secure data with an authentification level of "
                                + authenticationLevel + ". Scan NFC tag to set to maximum.");
                    }
                });
                if(authenticationLevel == 0){
                    authTimer.cancel();
                }
            }
        }, 5000, 5000);

    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, nfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

}
