package dao;

import database.DBConnection;
import model.TransactionRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Database operations for the transaction types supported by the Finova schema. */
public class TransactionDAO {
    private static final String TRANSACTION_SELECT =
            "SELECT t.*, COALESCE(src.club_id, dst.club_id) AS club_id " +
            "FROM transactions t " +
            "LEFT JOIN club_accounts src ON src.account_no = t.from_account " +
            "LEFT JOIN club_accounts dst ON dst.account_no = t.to_account ";

    public void save(Connection conn, TransactionRecord txn) throws SQLException {
        String sql = "INSERT INTO transactions " +
                "(txn_id, from_account, to_account, txn_type, amount, sponsor_name, bank_ref_id, " +
                "purpose, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txn.getTxnId());
            ps.setString(2, txn.getFromAccount());
            ps.setString(3, txn.getToAccount());
            ps.setString(4, txn.getTxnType());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setString(6, txn.getSponsorName());
            ps.setString(7, txn.getBankRefId());
            ps.setString(8, txn.getPurpose());
            ps.setString(9, txn.getStatus());
            ps.executeUpdate();
        }
    }

    public void save(TransactionRecord txn) {
        try (Connection conn = DBConnection.getConnection()) {
            save(conn, txn);
        } catch (SQLException e) {
            throw new RuntimeException("Could not save transaction: " + e.getMessage(), e);
        }
    }

    public List<TransactionRecord> findAll() {
        return query(TRANSACTION_SELECT + "ORDER BY t.txn_date DESC", null);
    }

    public List<TransactionRecord> findByClubId(String clubId) {
        String sql = TRANSACTION_SELECT +
                "WHERE src.club_id = ? OR dst.club_id = ? ORDER BY t.txn_date DESC";
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            ps.setString(2, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load club transactions: " + e.getMessage(), e);
        }
        return list;
    }

    public List<TransactionRecord> findSponsorInflowsAll() {
        return query(TRANSACTION_SELECT +
                "WHERE t.txn_type = 'SPONSOR_INFLOW' AND t.status = 'SUCCESS' ORDER BY t.txn_date DESC", null);
    }

    public List<String> findSponsorDirectory() {
        String sql = "SELECT DISTINCT sponsor_name FROM transactions " +
                "WHERE txn_type = 'SPONSOR_INFLOW' AND status = 'SUCCESS' " +
                "AND sponsor_name IS NOT NULL ORDER BY sponsor_name";
        List<String> sponsors = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) sponsors.add(rs.getString("sponsor_name"));
        } catch (SQLException e) {
            throw new RuntimeException("Could not load sponsor directory: " + e.getMessage(), e);
        }
        return sponsors;
    }

    public List<TransactionRecord> findRecentByAccount(String accountNo, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Limit must be greater than zero.");
        String sql = TRANSACTION_SELECT +
                "WHERE t.from_account = ? OR t.to_account = ? ORDER BY t.txn_date DESC LIMIT ?";
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNo);
            ps.setString(2, accountNo);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load recent transactions: " + e.getMessage(), e);
        }
        return list;
    }

    private List<TransactionRecord> query(String sql, String parameter) {
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parameter != null) ps.setString(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load transactions: " + e.getMessage(), e);
        }
        return list;
    }

    private TransactionRecord mapRow(ResultSet rs) throws SQLException {
        Timestamp time = rs.getTimestamp("txn_date");
        if (time == null) throw new SQLException("Transaction row has no transaction date.");
        LocalDateTime date = time.toLocalDateTime();
        return new TransactionRecord(rs.getString("txn_id"), rs.getString("club_id"),
                rs.getString("from_account"), rs.getString("to_account"), rs.getString("txn_type"),
                rs.getBigDecimal("amount"), rs.getString("sponsor_name"), rs.getString("bank_ref_id"),
                rs.getString("purpose"), rs.getString("status"), date);
    }
}
