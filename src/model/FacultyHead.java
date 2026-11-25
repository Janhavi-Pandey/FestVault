package model;

public class FacultyHead extends User {
    private final String clubName;

    public FacultyHead(int userId, String username, String fullName, String clubId,
                       int failedAttempts, boolean blocked, String clubName) {
        super(userId, username, fullName, "FACULTY", clubId, failedAttempts, blocked);
        this.clubName = clubName;
    }

    public FacultyHead(String username, String facultyName, String clubId, String clubName) {
        this(0, username, facultyName, clubId, 0, false, clubName);
    }

    public String getClubName() { return clubName; }
    @Override public String getDashboardTitle() { return "FACULTY HEAD - " + getName() + " | " + clubName; }
}
