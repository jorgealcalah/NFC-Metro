package ccm.rfid.nfc_metro;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ccm.rfid.nfc_metro.Instantes.FirestoreInstance;
import ccm.rfid.nfc_metro.Instantes.NFCInstance;
import ccm.rfid.nfc_metro.Models.Record;

public class RechargeActivity extends AppCompatActivity {

    private static final String TAG = "nfcinventory_simple";

    private NFCInstance nfcInstance;
    private TextView mCredit;
    private EditText mRecharge;
    private AlertDialog nfcDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge);

        // Initialize NFC Adapter
        nfcInstance = initNFCAdapter(this);

        // Get Layout fields
        mCredit = findViewById(R.id.recharge_quantity_textView);
        mRecharge = findViewById(R.id.recharge_textInputEditText);

        findViewById(R.id.recharge_back_button).setOnClickListener(v -> finish());
        findViewById(R.id.recharge_button).setOnClickListener(mTagWrite);
        findViewById(R.id.read_button).setOnClickListener(mTagRead);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!nfcInstance.isWriteMode()) {
            // Currently in tag READING mode
            resolveReadIntent(intent);
        } else {
            // Currently in tag WRITING mode
            resolveWriteIntent(intent);
        }
    }

    private void resolveReadIntent(Intent intent){
        String action = intent.getAction();
        if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);
            try {
                mfc.connect();
                // Auth
                int sector = mfc.blockToSector(NFCInstance.USER_CREDIT_BLOCK);
                byte[] dataKey = NFCInstance.hexStringToByteArray(NFCInstance.DEFAULT_KEY);
                boolean auth = mfc.authenticateSectorWithKeyA(sector, dataKey); // Or with Key B

                byte[] cleanIDByte = mfc.readBlock(NFCInstance.USER_ID_BLOCK);
                String cleanUserHex = NFCInstance.getHexString(cleanIDByte, cleanIDByte.length);

                if(cleanUserHex.equals(NFCInstance.DEFAULT_USERID)){
                    mfc.close();
                    nfcDialog.cancel();
                    Toast.makeText(this,
                            R.string.no_register,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Check
                if(auth) {
                    byte[] creditByte = mfc.readBlock(NFCInstance.USER_CREDIT_BLOCK);
                    String creditHex = NFCInstance.getHexString(creditByte, creditByte.length);
                    int credit = NFCInstance.hexStringToInt(creditHex);
                    mCredit.setText(Integer.toString(credit));
                } else{
                    Toast.makeText(this,
                            R.string.wrong_password,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                mfc.close();
                nfcDialog.cancel();
            } catch (IOException e){
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    private void resolveWriteIntent(Intent intent){
        String action = intent.getAction();
        if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);
            try {
                mfc.connect();
                // Auth
                int sector = mfc.blockToSector(NFCInstance.USER_CREDIT_BLOCK);
                byte[] dataKey = NFCInstance.hexStringToByteArray(NFCInstance.DEFAULT_KEY);
                boolean auth = mfc.authenticateSectorWithKeyA(sector, dataKey); // Or with Key B

                byte[] cleanIDByte = mfc.readBlock(NFCInstance.USER_ID_BLOCK);
                String cleanUserHex = NFCInstance.getHexString(cleanIDByte, cleanIDByte.length);

                if(cleanUserHex.equals(NFCInstance.DEFAULT_USERID)){
                    mfc.close();
                    nfcDialog.cancel();
                    Toast.makeText(this,
                            R.string.no_register,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Check
                if(auth) {
                    // Read Credit block
                    byte[] creditByte = mfc.readBlock(NFCInstance.USER_CREDIT_BLOCK);
                    String creditHex = NFCInstance.getHexString(creditByte, creditByte.length);
                    int credit = NFCInstance.hexStringToInt(creditHex);
                    int rechargeAmount = Integer.parseInt(mRecharge.getText().toString());
                    int totalCredit = rechargeAmount + credit;
                    String totalCreditHex = NFCInstance.intToHexString(totalCredit, NFCInstance.USER_CREDIT_BLOCK);
                    // Update user card
                    byte[] dataToWrite = NFCInstance.hexStringToByteArray(totalCreditHex);
                    mfc.writeBlock(NFCInstance.USER_CREDIT_BLOCK, dataToWrite);

                    // Read User block
                    byte[] userIDByte = mfc.readBlock(NFCInstance.USER_ID_BLOCK);
                    String userIDHex = NFCInstance.getHexString(userIDByte, userIDByte.length);
                    byte[] userIDByte2 = mfc.readBlock(NFCInstance.USER_ID_BLOCK+1);
                    String userIDHex2 = NFCInstance.getHexString(userIDByte2, 4);
                    String userID = NFCInstance.hexStringToString(userIDHex) +
                            NFCInstance.hexStringToString(userIDHex2);

                    // Update user database
                    DocumentReference userDocument = FirestoreInstance.getDb()
                            .collection(FirestoreInstance.METRO_COLLECTION).document(userID);
                    Map<String, Object> data = new HashMap<>();
                    data.put("credit", totalCredit);
                    userDocument.set(data, SetOptions.merge());

                    // Add action to user history
                    CollectionReference recordCollection = userDocument
                            .collection(FirestoreInstance.RECORD_COLLECTION);
                    Record creationRecord = new Record(
                            FirestoreInstance.ACTION_RECHARGE,
                            rechargeAmount,
                            Timestamp.now());
                    recordCollection.document().set(creationRecord);

                    mCredit.setText(Integer.toString(totalCredit));
                    Toast.makeText(this,
                            R.string.write_successful,
                            Toast.LENGTH_LONG).show();
                } else{
                    Toast.makeText(this,
                            R.string.wrong_password,
                            Toast.LENGTH_LONG).show();
                }
                mRecharge.getText().clear();

                mfc.close();
                nfcDialog.cancel();
            } catch (IOException e){
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    private NFCInstance initNFCAdapter(Context context){
        return new NFCInstance(context, () -> {
            finish();
            System.exit(0);
        });
    }

    /*  Button listeners  */
    private View.OnClickListener mTagWrite = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            nfcInstance.enableTagWriteMode(RechargeActivity.this);
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    RechargeActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(getString(R.string.ready_to_write_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            (dialog, id) -> dialog.cancel())
                    .setOnCancelListener(
                            dialog -> nfcInstance.enableTagReadMode(RechargeActivity.this));
            nfcDialog = builder.create();
            nfcDialog.show();
        }
    };

    private View.OnClickListener mTagRead = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            nfcInstance.enableTagReadMode(RechargeActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    RechargeActivity.this)
                    .setTitle(getString(R.string.ready_to_read))
                    .setMessage(getString(R.string.ready_to_read_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            (dialog, id) -> dialog.cancel())
                    .setOnCancelListener(
                            dialog -> nfcInstance.enableTagReadMode(RechargeActivity.this));
            nfcDialog = builder.create();
            nfcDialog.show();
        }
    };
}