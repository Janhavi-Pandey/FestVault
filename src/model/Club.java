package model;

/** Club profile with names of its assigned Faculty Head and Student Lead. */
public class Club {
    private final String clubId;
    private final String clubName;
    private final String facultyName;
    private final String studentName;

    public Club(String clubId, String clubName, String facultyName, String studentName) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.facultyName = facultyName;
        this.studentName = studentName;
    }

    public String getClubId() { return clubId; }
    public String getClubName() { return clubName; }
    public String getFacultyName() { return facultyName; }
    public String getStudentName() { return studentName; }
}
