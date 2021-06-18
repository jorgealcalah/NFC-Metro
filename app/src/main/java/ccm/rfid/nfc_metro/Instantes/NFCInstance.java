package ccm.rfid.nfc_metro.Instantes;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.nfc.tech.MifareClassic;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.Callable;

public class NFCInstance {
    private NfcAdapter adapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mReadWriteTagFilters;
    private String[][]mTechList;

    private boolean isWriteMode;

    public static final int USER_ID_BLOCK = 16; // Sector 4 block 0
    public static final int USER_CREDIT_BLOCK = 18; // Sector 4 block 2

    public static final String DEFAULT_KEY = "D3F7D3F7D3F7";
    public static final String DEFAULT_USERID = "00000000000000000000000000000000";

    public NFCInstance(Context context, NfcCallback onNfcNull) {
        // get an instance of the context's cached NfcAdapter
        adapter = NfcAdapter.getDefaultAdapter(context);
        // if null is returned this demo cannot run. Use this check if the
        // "required" parameter of <uses-feature> in the manifest is not set
        if (adapter == null)
        {
            Toast.makeText(context,
                    "Su dispositivo no soporta NFC. No se puede correr la aplicación.",
                    Toast.LENGTH_LONG).show();
            // When NfcAdapter is null
            onNfcNull.onNfcNull();
            return;
        }
        mNfcPendingIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        IntentFilter mifareDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            mifareDetected.addDataType("application/com.e.mifarecontrol");
        } catch (IntentFilter.MalformedMimeTypeException e)
        {
            throw new RuntimeException("No se pudo añadir un tipo MIME.", e);
        }
        // Create intent filter to detect any MIFARE NFC tag
        mReadWriteTagFilters = new IntentFilter[] { mifareDetected };
        mTechList = new String[][] { new String[] { MifareClassic.class.getName() } };
    }

    public void checkNfcEnabled(AlertDialog.Builder alertDialog)
    {
        Boolean nfcEnabled = adapter.isEnabled();
        if (!nfcEnabled)
        {
            alertDialog.create().show();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String getHexString(byte[] b, int length)
    {
        String result = "";
        Locale loc = Locale.getDefault();
        for (int i = 0; i < length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            result += ""; //Poner espacio si se quiere separar de dos en dos caracteres hex
        }
        return result.toUpperCase(loc);
    }

    public static String stringToHexString(String s){
        StringBuffer sb = new StringBuffer();
        //Converting string to character array
        char ch[] = s.toCharArray();
        for(int i = 0; i < ch.length; i++) {
            String hexString = Integer.toHexString(ch[i]);
            sb.append(hexString);
        }
        return sb.toString();
    }

    public static String byteArrayToString(byte[] hex){
        try {
            return new String(hex, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String hexStringToString(String hex){
        byte[] bytes = hexStringToByteArray(hex);
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String intToHexString(int i, int block){
        String intHex = Integer.toHexString(i);
        String blockHex = Integer.toHexString(block);
        String leadIntHex = ("00000000" + intHex).substring(intHex.length());
        String leadBlockHex = ("00" + blockHex).substring(blockHex.length());
        String invIntHex = Integer.toHexString((-1*i)-1);
        String invBlockHex = Integer.toHexString((-1*block)-1).substring(6);
        return leadIntHex + invIntHex + leadIntHex +
                leadBlockHex + invBlockHex + leadBlockHex + invBlockHex;
    }

    public static int hexStringToInt(String hex){
        String intStr = hex.substring(0, 8);
        return Integer.parseInt(intStr, 16);
    }

    public void enableTagWriteMode(Activity activity)
    {
        isWriteMode = true;
        adapter.enableForegroundDispatch(activity, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }
    public void enableTagReadMode(Activity activity)
    {
        isWriteMode = false;
        adapter.enableForegroundDispatch(activity, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    public NfcAdapter getAdapter() {
        return adapter;
    }

    public PendingIntent getNfcPendingIntent() {
        return mNfcPendingIntent;
    }

    public IntentFilter[] getReadWriteTagFilters() {
        return mReadWriteTagFilters;
    }

    public String[][] getTechList() {
        return mTechList;
    }

    public boolean isWriteMode() {
        return isWriteMode;
    }

    public interface NfcCallback{
        void onNfcNull();
    }
}