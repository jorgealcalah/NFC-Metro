package ccm.rfid.nfc_metro;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import android.content.Intent;

import ccm.rfid.nfc_metro.Instantes.FirestoreInstance;
import ccm.rfid.nfc_metro.Instantes.NFCInstance;

public class MainActivity extends AppCompatActivity {

    private NFCInstance nfcInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize NFC Adapter
        nfcInstance = initNFCAdapter(this);

        // Instantiate Firestore
        FirestoreInstance.instantiate();

        // Set buttons listener
        findViewById(R.id.pay_activity_button).setOnClickListener(v -> changeActivity(PayActivity.class));
        findViewById(R.id.recharge_activity_button).setOnClickListener(v -> changeActivity(RechargeActivity.class));
        findViewById(R.id.history_activity_button).setOnClickListener(v -> changeActivity(HistoryActivity.class));
        findViewById(R.id.register_activity_button).setOnClickListener(v -> changeActivity(RegisterActivity.class));
        findViewById(R.id.exit_button).setOnClickListener(v -> finish());
    }

    private void changeActivity(Class<?> c){
        Intent activityToChange = new Intent(getApplicationContext(), c);
        startActivity(activityToChange);
    }

    private NFCInstance initNFCAdapter(Context context){
        return new NFCInstance(context, () -> {
            finish();
            System.exit(0);
        });
    }
}