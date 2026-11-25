package dao;

import database.DBConnection;
import repository.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClubDAO implements Repository<String[], String> {

    @Override
    public void save(String[] clubData) {
        String sql = "INSERT INTO clubs (club_id, club_name) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubData[0]);
            ps.setString(2, clubData[1]);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save club: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<String[]> findById(String clubId) {
        String sql = "SELECT club_id, club_name FROM clubs WHERE club_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{rs.getString("club_id"), rs.getString("club_name")});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch club: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<String[]> findAll() {
        String sql = "SELECT club_id, club_name FROM clubs ORDER BY club_id";
        List<String[]> clubs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clubs.add(new String[]{rs.getString("club_id"), rs.getString("club_name")});
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list clubs: " + e.getMessage(), e);
        }
        return clubs;
    }
}
