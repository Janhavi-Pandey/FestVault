package model;

public class FacultyHead extends User {
    private final String clubId;

    public FacultyHead(String userId, String name, String email, String clubId) {
        super(userId, name, email, "FACULTY");
        this.clubId = clubId;
    }

    public String getClubId() { return clubId; }

    @Override
    public String getDashboardTitle() {
        return "FACULTY HEAD MENU - " + clubId;
    }
}
