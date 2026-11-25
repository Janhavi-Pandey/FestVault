package model;

public class StudentClubHead extends User {
    private final String clubId;
    private final String passId;

    public StudentClubHead(String userId, String name, String email, String clubId, String passId) {
        super(userId, name, email, "STUDENT");
        this.clubId = clubId;
        this.passId = passId;
    }

    public String getClubId() { return clubId; }
    public String getPassId() { return passId; }

    @Override
    public String getDashboardTitle() {
        return "STUDENT CLUB HEAD MENU - " + clubId;
    }
}
