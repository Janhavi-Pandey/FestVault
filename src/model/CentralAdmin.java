package model;

public class CentralAdmin extends User {
    public CentralAdmin(int userId, String username, String fullName,
                        String clubId, int failedAttempts, boolean blocked) {
        super(userId, username, fullName, "ADMIN", clubId, failedAttempts, blocked);
    }

    public CentralAdmin(int userId, String username, String fullName, int failedAttempts, boolean blocked) {
        this(userId, username, fullName, null, failedAttempts, blocked);
    }

    public CentralAdmin(String username, String name) {
        this(0, username, name, 0, false);
    }

    @Override public String getDashboardTitle() { return "CENTRAL ADMIN"; }
}
