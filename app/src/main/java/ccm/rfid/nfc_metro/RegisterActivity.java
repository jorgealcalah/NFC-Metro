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
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.io.IOException;

import ccm.rfid.nfc_metro.Instantes.FirestoreInstance;
import ccm.rfid.nfc_metro.Instantes.NFCInstance;
import ccm.rfid.nfc_metro.Models.Record;
import ccm.rfid.nfc_metro.Models.User;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "nfcinventory_simple";

    private NFCInstance nfcInstance;

    private AlertDialog nfcDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize NFC Adapter
        nfcInstance = initNFCAdapter(this);

        findViewById(R.id.register_back_button).setOnClickListener(v -> finish());
        findViewById(R.id.read_register_button).setOnClickListener(mTagWrite);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (nfcInstance.isWriteMode()) {
            // Currently in tag READING mode
            resolveWriteIntent(intent);
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
                int trailerBlock = NFCInstance.USER_CREDIT_BLOCK + 1;

                byte[] dataKey = NFCInstance.hexStringToByteArray(NFCInstance.DEFAULT_KEY);
                boolean auth = mfc.authenticateSectorWithKeyA(sector, dataKey); // Or with Key B

                byte[] cleanIDByte = mfc.readBlock(NFCInstance.USER_ID_BLOCK);
                String cleanUserHex = NFCInstance.getHexString(cleanIDByte, cleanIDByte.length);

                if(!cleanUserHex.equals(NFCInstance.DEFAULT_USERID)){
                    mfc.close();
                    nfcDialog.cancel();
                    Toast.makeText(this,
                            R.string.already_registered,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (auth) {
                    DocumentReference userDocument = FirestoreInstance.getDb()
                            .collection(FirestoreInstance.METRO_COLLECTION).document();

                    // Set user id in the tag
                    String userID = userDocument.getId();
                    String userID1 = userID.substring(0, 16);
                    String userID2 = userID.substring(16);
                    String userIDHex1 = NFCInstance.stringToHexString(userID1);
                    String userIDHex2 = NFCInstance.stringToHexString(userID2) + "000000000000000000000000";
                    byte[] userIDByte1 = NFCInstance.hexStringToByteArray(userIDHex1);
                    byte[] userIDByte2 = NFCInstance.hexStringToByteArray(userIDHex2);
                    mfc.writeBlock(NFCInstance.USER_ID_BLOCK, userIDByte1);
                    mfc.writeBlock(NFCInstance.USER_ID_BLOCK+1, userIDByte2);

                    // Set credit in the tag
                    String creditHex = NFCInstance.intToHexString(0, NFCInstance.USER_CREDIT_BLOCK);
                    byte[] creditByte = NFCInstance.hexStringToByteArray(creditHex);
                    mfc.writeBlock(NFCInstance.USER_CREDIT_BLOCK, creditByte);

                    // Register new user
                    User newUser = new User(userID, 0);
                    userDocument.set(newUser);

                    // Create Creation record
                    CollectionReference recordCollection = userDocument
                            .collection(FirestoreInstance.RECORD_COLLECTION);
                    Record creationRecord = new Record(
                            FirestoreInstance.ACTION_CREATE,
                            0,
                            Timestamp.now());
                    recordCollection.document().set(creationRecord);

//                    String password = mPassword.getText().toString();
//                    String hexPass = NFCInstance.stringToHexString(password);
//                    byte[] trailerData = mfc.readBlock(trailerBlock);
//                    String trailerHex = NFCInstance.getHexString(trailerData, trailerData.length);
//                    String newPasswordHex = hexPass + trailerHex.substring(12);
//                    Log.i(TAG, newPasswordHex);
//                    byte[] mewPasswordByte = NFCInstance.hexStringToByteArray(newPasswordHex);
//                    mfc.writeBlock(trailerBlock, mewPasswordByte);
//                    Log.i(TAG, "Password changed");
                } else{
                    Toast.makeText(this,
                            R.string.wrong_password,
                            Toast.LENGTH_LONG).show();
                }
                mfc.close();
                nfcDialog.cancel();
            } catch (IOException e){
                Log.e(TAG, e.toString());
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
            nfcInstance.enableTagWriteMode(RegisterActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    RegisterActivity.this)
                    .setTitle(getString(R.string.ready_to_read))
                    .setMessage(getString(R.string.ready_to_read_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            (dialog, id) -> dialog.cancel())
                    .setOnCancelListener(
                            dialog -> nfcInstance.enableTagReadMode(RegisterActivity.this));
            nfcDialog = builder.create();
            nfcDialog.show();
        }
    };
}