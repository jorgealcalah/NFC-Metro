package ccm.rfid.nfc_metro.Instantes;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreInstance {
    private  static FirestoreInstance singleton;
    private static FirebaseFirestore db;

    public static final String METRO_COLLECTION = "MetroCard";
    public static final String RECORD_COLLECTION = "UserRecord";

    public static final String ACTION_RECHARGE = "Recarga";
    public static final String ACTION_PAY = "Paga";
    public static final String ACTION_CREATE = "Creacion";

    public FirestoreInstance(){
        if(getDb() == null){

            db = FirebaseFirestore.getInstance();
        }
    }

    public static void instantiate(){
        if (singleton == null)
            singleton = new FirestoreInstance();
    }

    public static FirestoreInstance getFirestore(){
        if (singleton == null) instantiate();

        return singleton;
    }

    public static FirebaseFirestore getDb() {
        return db;
    }
}
