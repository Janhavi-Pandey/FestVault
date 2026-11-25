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

public class TransactionDAO {

    public void save(Connection conn, TransactionRecord txn) throws SQLException {
        String sql = "INSERT INTO transactions " +
                     "(txn_id, from_account, to_account, txn_type, amount, sponsor_name, bank_ref_id, purpose, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new RuntimeException("Failed to save transaction: " + e.getMessage(), e);
        }
    }

    public List<TransactionRecord> findRecentByAccount(String accountNo, int limit) {
        String sql = "SELECT * FROM transactions WHERE from_account = ? OR to_account = ? " +
                     "ORDER BY txn_date DESC LIMIT ?";
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNo);
            ps.setString(2, accountNo);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("txn_date");
                    LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    list.add(new TransactionRecord(
                            rs.getString("txn_id"),
                            rs.getString("from_account"),
                            rs.getString("to_account"),
                            rs.getString("txn_type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("sponsor_name"),
                            rs.getString("bank_ref_id"),
                            rs.getString("purpose"),
                            rs.getString("status"),
                            ldt
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions: " + e.getMessage(), e);
        }
        return list;
    }
}
