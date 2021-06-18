package ccm.rfid.nfc_metro.Models;

import com.google.firebase.Timestamp;

public class Record {

    private String action;
    private int amount;
    private Timestamp timestamp;

    public Record() {}

    public Record(String action, int amount, Timestamp timeStamp) {
        this.action = action;
        this.amount = amount;
        this.timestamp = timeStamp;
    }

    public String getAction() {
        return action;
    }

    public int getAmount() {
        return amount;
    }

    public Timestamp getTimeStamp() {
        return timestamp;
    }

    public void setAction(String action) { this.action = action; }

    public void setAmount(int amount) { this.amount = amount; }

    public void setTimeStamp(Timestamp timeStamp) { this.timestamp = timeStamp; }
}
