package model;

/** Common parent for the three Finova application roles. */
public abstract class User {
    private final int userId;
    private final String username;
    private final String fullName;
    private final String role;
    private final String clubId;
    private final int failedAttempts;
    private final boolean blocked;

    protected User(int userId, String username, String fullName, String role, String clubId,
                   int failedAttempts, boolean blocked) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.clubId = clubId;
        this.failedAttempts = failedAttempts;
        this.blocked = blocked;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getName() { return fullName; }
    public String getRole() { return role; }
    public String getClubId() { return clubId; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isBlocked() { return blocked; }
    public abstract String getDashboardTitle();
}
