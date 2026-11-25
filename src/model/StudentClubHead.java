package model;

public class StudentClubHead extends User {
    private final String clubName;

    public StudentClubHead(int userId, String username, String fullName, String clubId,
                           int failedAttempts, boolean blocked, String clubName) {
        super(userId, username, fullName, "STUDENT", clubId, failedAttempts, blocked);
        this.clubName = clubName;
    }

    public StudentClubHead(String username, String studentName, String clubId, String clubName) {
        this(0, username, studentName, clubId, 0, false, clubName);
    }

    public String getClubName() { return clubName; }
    @Override public String getDashboardTitle() { return "STUDENT CLUB HEAD - " + getName() + " | " + clubName; }
}
