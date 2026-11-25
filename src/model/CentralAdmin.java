package model;

public class CentralAdmin extends User {
    public CentralAdmin(String userId, String name, String email) {
        super(userId, name, email, "ADMIN");
    }

    @Override
    public String getDashboardTitle() {
        return "CENTRAL ADMIN MENU - FEST CONVENER";
    }
}
