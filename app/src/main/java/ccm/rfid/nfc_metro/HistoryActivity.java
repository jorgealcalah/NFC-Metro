package ccm.rfid.nfc_metro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ccm.rfid.nfc_metro.Instantes.FirestoreInstance;
import ccm.rfid.nfc_metro.Instantes.NFCInstance;
import ccm.rfid.nfc_metro.Models.Record;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "nfcinventory_simple";

    private RecyclerView mRecyclerView;
    private RecordAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    private NFCInstance nfcInstance;
    private AlertDialog nfcDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize NFC Adapter
        nfcInstance = initNFCAdapter(this);

        // Initialize recycler view
        mRecyclerView = findViewById(R.id.record_recyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new RecordAdapter();

        // Configure recycler view
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.history_back_button).setOnClickListener(v -> finish());
        findViewById(R.id.read_history_button).setOnClickListener(mTagRead);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!nfcInstance.isWriteMode()) {
            // Currently in tag READING mode
            resolveReadIntent(intent);
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
                    // Read User block
                    byte[] userIDByte = mfc.readBlock(NFCInstance.USER_ID_BLOCK);
                    String userIDHex = NFCInstance.getHexString(userIDByte, userIDByte.length);
                    byte[] userIDByte2 = mfc.readBlock(NFCInstance.USER_ID_BLOCK+1);
                    String userIDHex2 = NFCInstance.getHexString(userIDByte2, 4);
                    String userID = NFCInstance.hexStringToString(userIDHex) +
                            NFCInstance.hexStringToString(userIDHex2);

                    // Retrieve user record
                    CollectionReference collection = FirestoreInstance.getDb()
                            .collection(FirestoreInstance.METRO_COLLECTION);
                    DocumentReference userDocument = collection.document(userID);
                    CollectionReference recordCollection = userDocument
                            .collection(FirestoreInstance.RECORD_COLLECTION);
                    recordCollection.orderBy("timeStamp", Query.Direction.DESCENDING)
                            .get().addOnCompleteListener(task -> {

                        if(task.isSuccessful()){
                            List<Record> records = new ArrayList<>();
                            for(QueryDocumentSnapshot document: task.getResult()){
                                Record record = document.toObject(Record.class);
                                records.add(record);
                            }
                            mAdapter.setRecords(records);
                        }
                    });

                } else{
                    Toast.makeText(this,
                            R.string.wrong_password,
                            Toast.LENGTH_LONG).show();
                }
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
    private View.OnClickListener mTagRead = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            nfcInstance.enableTagReadMode(HistoryActivity.this);
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    HistoryActivity.this)
                    .setTitle(getString(R.string.ready_to_read))
                    .setMessage(getString(R.string.ready_to_read_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            (dialog, id) -> dialog.cancel())
                    .setOnCancelListener(
                            dialog -> nfcInstance.enableTagReadMode(HistoryActivity.this));
            nfcDialog = builder.create();
            nfcDialog.show();
        }
    };
}