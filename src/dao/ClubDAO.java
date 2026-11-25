package dao;

import database.DBConnection;
import model.CentralAdmin;
import model.Club;
import model.FacultyHead;
import model.StudentClubHead;
import exception.AccountBlockedException;
import repository.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Database operations for club profiles and the three credential roles. */
public class ClubDAO implements Repository<Club, String> {
    private static final String DEFAULT_CLUB_ID = "CLB-01";
    private static final String DEFAULT_CLUB_NAME = "Coding Club";
    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Override
    public void save(Club club) {
        try (Connection conn = DBConnection.getConnection()) {
            registerClub(conn, club.getClubId(), club.getClubName());
        } catch (SQLException e) {
            throw new RuntimeException("Could not register club: " + e.getMessage(), e);
        }
    }

    public void registerClub(String clubId, String clubName) {
        try (Connection conn = DBConnection.getConnection()) {
            registerClub(conn, clubId, clubName);
        } catch (SQLException e) {
            throw new RuntimeException("Could not register club: " + e.getMessage(), e);
        }
    }

    public void registerDefaultClub() {
        registerClub(DEFAULT_CLUB_ID, DEFAULT_CLUB_NAME);
    }

    public void registerClub(Connection conn, String clubId, String clubName) throws SQLException {
        String sql = "INSERT INTO clubs (club_id, club_name) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            ps.setString(2, clubName);
            ps.executeUpdate();
        }
    }

    public FacultyHead createOrUpdateFacultyUser(String clubId, String username, String fullName, String password) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                FacultyHead user = createOrUpdateFacultyUser(conn, clubId, username, fullName, password);
                conn.commit();
                return user;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save Faculty Head credentials: " + e.getMessage(), e);
        }
    }

    public FacultyHead createOrUpdateFacultyUser(Connection conn, String clubId, String username,
                                                  String fullName, String password) throws SQLException {
        UserRow row = saveRoleUser(conn, "FACULTY", clubId, username, fullName, password);
        return new FacultyHead(row.userId, row.username, row.fullName, row.clubId,
                row.failedAttempts, row.blocked, row.clubName);
    }

    public StudentClubHead createOrUpdateStudentUser(String clubId, String username, String fullName,
                                                      String password) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StudentClubHead user = createOrUpdateStudentUser(conn, clubId, username, fullName, password);
                conn.commit();
                return user;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save Student Club Head credentials: " + e.getMessage(), e);
        }
    }

    public StudentClubHead createOrUpdateStudentUser(Connection conn, String clubId, String username,
                                                      String fullName, String password) throws SQLException {
        UserRow row = saveRoleUser(conn, "STUDENT", clubId, username, fullName, password);
        return new StudentClubHead(row.userId, row.username, row.fullName, row.clubId,
                row.failedAttempts, row.blocked, row.clubName);
    }

    /** Compatibility entry point; credential updates now belong in the users table. */
    public void updateStudentCredentials(String clubId, String studentName, String studentUsername,
                                         String studentPassword) {
        createOrUpdateStudentUser(clubId, studentUsername, studentName, studentPassword);
    }

    public Optional<CentralAdmin> authenticateAdmin(String username, String password) {
        return authenticate("ADMIN", null, username, password, true)
                .map(row -> new CentralAdmin(row.userId, row.username, row.fullName, row.clubId,
                        row.failedAttempts, row.blocked));
    }

    public Optional<FacultyHead> authenticateFaculty(String clubId, String username, String password) {
        return authenticate("FACULTY", clubId, username, password, false)
                .map(row -> new FacultyHead(row.userId, row.username, row.fullName, row.clubId,
                        row.failedAttempts, row.blocked, row.clubName));
    }

    public Optional<StudentClubHead> authenticateStudent(String clubId, String username, String password) {
        return authenticate("STUDENT", clubId, username, password, false)
                .map(row -> new StudentClubHead(row.userId, row.username, row.fullName, row.clubId,
                        row.failedAttempts, row.blocked, row.clubName));
    }

    public int incrementFailedAttempts(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int attempts = incrementFailedAttempts(conn, userId, false);
                conn.commit();
                return attempts;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update failed login attempts: " + e.getMessage(), e);
        }
    }

    public void resetFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts = 0, is_blocked = FALSE WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not reset login attempts: " + e.getMessage(), e);
        }
    }

    public void setStudentBlocked(int userId, boolean blocked) {
        String sql = "UPDATE users SET is_blocked = ?, failed_attempts = IF(?, failed_attempts, 0) " +
                "WHERE user_id = ? AND role = 'STUDENT'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, blocked);
            ps.setBoolean(2, blocked);
            ps.setInt(3, userId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Student user not found: " + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update Student user block status: " + e.getMessage(), e);
        }
    }

    public void blockStudentUser(int userId) {
        setStudentBlocked(userId, true);
    }

    public void unblockStudentUser(int userId) {
        setStudentBlocked(userId, false);
    }

    @Override
    public Optional<Club> findById(String clubId) {
        String sql = clubSelect() + " WHERE c.club_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapClub(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find club: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Club> findAll() {
        String sql = clubSelect() + " ORDER BY c.club_id";
        List<Club> clubs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) clubs.add(mapClub(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Could not list clubs: " + e.getMessage(), e);
        }
        return clubs;
    }

    private UserRow saveRoleUser(Connection conn, String role, String clubId, String username,
                                 String fullName, String password) throws SQLException {
        if (username == null || username.isBlank() || fullName == null || fullName.isBlank()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username, full name, and password are required.");
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT club_id FROM clubs WHERE club_id = ? FOR UPDATE")) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Club not found: " + clubId);
            }
        }

        Integer userId = null;
        String select = "SELECT user_id FROM users WHERE club_id = ? AND role = ? ORDER BY user_id LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, clubId);
            ps.setString(2, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) userId = rs.getInt("user_id");
            }
        }

        String passwordHash = hashPassword(password);
        if (userId == null) {
            String insert = "INSERT INTO users (username, full_name, role, password_hash, club_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, username);
                ps.setString(2, fullName);
                ps.setString(3, role);
                ps.setString(4, passwordHash);
                ps.setString(5, clubId);
                ps.executeUpdate();
            }
        } else {
            String update = "UPDATE users SET username = ?, full_name = ?, password_hash = ?, " +
                    "failed_attempts = 0, is_blocked = FALSE WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setString(1, username);
                ps.setString(2, fullName);
                ps.setString(3, passwordHash);
                ps.setInt(4, userId);
                ps.executeUpdate();
            }
        }
        return findUserRow(conn, role, clubId, username)
                .orElseThrow(() -> new SQLException("Saved user could not be reloaded."));
    }

    private Optional<UserRow> authenticate(String role, String clubId, String username,
                                           String password, boolean allowSeededAdmin) {
        if (username == null || username.isBlank() || password == null) return Optional.empty();
        try (Connection conn = DBConnection.getConnection()) {
            Optional<UserRow> found = findUserRow(conn, role, clubId, username);
            if (found.isEmpty()) return Optional.empty();
            UserRow row = found.get();
            if (row.blocked) {
                throw new AccountBlockedException("This user is blocked after three failed login attempts.");
            }

            String submittedHash = hashPassword(password);
            boolean seededAdminPassword = allowSeededAdmin && password.equals(row.passwordHash);
            if (!seededAdminPassword && !constantTimeEquals(row.passwordHash, submittedHash)) {
                int attempts = incrementFailedAttempts(conn, row.userId, "STUDENT".equals(role));
                if ("STUDENT".equals(role) && attempts >= MAX_FAILED_ATTEMPTS) {
                    throw new AccountBlockedException("Three incorrect attempts. This user is now blocked.");
                }
                return Optional.empty();
            }
            if (seededAdminPassword) {
                String update = "UPDATE users SET password_hash = ? WHERE user_id = ? AND password_hash = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, submittedHash);
                    ps.setInt(2, row.userId);
                    ps.setString(3, row.passwordHash);
                    ps.executeUpdate();
                }
            }
            resetFailedAttempts(conn, row.userId);
            row.failedAttempts = 0;
            row.blocked = false;
            return Optional.of(row);
        } catch (SQLException e) {
            throw new RuntimeException("Could not authenticate user: " + e.getMessage(), e);
        }
    }

    private Optional<UserRow> findUserRow(Connection conn, String role, String clubId, String username)
            throws SQLException {
        String sql = "SELECT u.user_id, u.username, u.full_name, u.role, u.club_id, " +
                "u.failed_attempts, u.is_blocked, u.password_hash, c.club_name " +
                "FROM users u LEFT JOIN clubs c ON c.club_id = u.club_id " +
                "WHERE u.role = ? AND u.username = ?" +
                (clubId == null ? "" : " AND u.club_id = ?") +
                " ORDER BY u.user_id LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, username);
            if (clubId != null) ps.setString(3, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new UserRow(rs.getInt("user_id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("club_id"), rs.getString("club_name"),
                        rs.getInt("failed_attempts"), rs.getBoolean("is_blocked"),
                        rs.getString("password_hash")));
            }
        }
    }

    private int incrementFailedAttempts(Connection conn, int userId, boolean blockAtThreshold)
            throws SQLException {
        String update = "UPDATE users SET is_blocked = CASE WHEN failed_attempts >= ? " +
                "AND ? THEN TRUE ELSE is_blocked END, failed_attempts = failed_attempts + 1 " +
                "WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, MAX_FAILED_ATTEMPTS - 1);
            ps.setBoolean(2, blockAtThreshold);
            ps.setInt(3, userId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("User not found: " + userId);
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT failed_attempts FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("User not found: " + userId);
                return rs.getInt(1);
            }
        }
    }

    private void resetFailedAttempts(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET failed_attempts = 0, is_blocked = FALSE WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private String clubSelect() {
        return "SELECT c.club_id, c.club_name, " +
                "(SELECT u.full_name FROM users u WHERE u.club_id = c.club_id AND u.role = 'FACULTY' " +
                "ORDER BY u.user_id LIMIT 1) AS faculty_name, " +
                "(SELECT u.full_name FROM users u WHERE u.club_id = c.club_id AND u.role = 'STUDENT' " +
                "ORDER BY u.user_id LIMIT 1) AS student_name FROM clubs c";
    }

    private Club mapClub(ResultSet rs) throws SQLException {
        return new Club(rs.getString("club_id"), rs.getString("club_name"),
                rs.getString("faculty_name"), rs.getString("student_name"));
    }

    private static String hashPassword(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private static boolean constantTimeEquals(String storedHash, String submittedHash) {
        if (storedHash == null) return false;
        return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.US_ASCII),
                submittedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static final class UserRow {
        private final int userId;
        private final String username;
        private final String fullName;
        private final String clubId;
        private final String clubName;
        private int failedAttempts;
        private boolean blocked;
        private final String passwordHash;

        private UserRow(int userId, String username, String fullName, String clubId, String clubName,
                        int failedAttempts, boolean blocked, String passwordHash) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.clubId = clubId;
            this.clubName = clubName;
            this.failedAttempts = failedAttempts;
            this.blocked = blocked;
            this.passwordHash = passwordHash;
        }
    }
}
