package ccm.rfid.nfc_metro.Models;

public class User {

    private String id;
    private int credit;

    public User() {}

    public User(String id, int credit) {
        this.id = id;
        this.credit = credit;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public int getCredit() { return credit; }

    public void setCredit(int credit) { this.credit = credit; }
}
