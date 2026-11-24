package dao;

import database.DBConnection;
import model.AccountStatus;
import model.ClubAccount;
import model.EventSubAccount;
import model.MainClubAccount;
import repository.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDAO implements Repository<ClubAccount, String> {

    @Override
    public void save(ClubAccount account) {
        String sql = "INSERT INTO club_accounts (account_no, club_id, account_name, account_type, balance, locked_amount, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAccountNo());
            ps.setString(2, account.getClubId());
            ps.setString(3, account.getAccountName());
            ps.setString(4, account.getAccountType());
            ps.setBigDecimal(5, account.getBalance());
            ps.setBigDecimal(6, account.getLockedAmount());
            ps.setString(7, account.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save account: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ClubAccount> findById(String accountNo) {
        String sql = "SELECT account_no, club_id, account_name, account_type, balance, locked_amount, status " +
                     "FROM club_accounts WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<ClubAccount> findAll() {
        String sql = "SELECT account_no, club_id, account_name, account_type, balance, locked_amount, status " +
                     "FROM club_accounts ORDER BY account_no";
        List<ClubAccount> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load accounts: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ClubAccount> findByClubId(String clubId) {
        String sql = "SELECT account_no, club_id, account_name, account_type, balance, locked_amount, status " +
                     "FROM club_accounts WHERE club_id = ? ORDER BY account_no";
        List<ClubAccount> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load club accounts: " + e.getMessage(), e);
        }
        return list;
    }

    public void updateBalanceAndStatus(Connection conn, ClubAccount account) throws SQLException {
        String sql = "UPDATE club_accounts SET balance = ?, locked_amount = ?, status = ? WHERE account_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, account.getBalance());
            ps.setBigDecimal(2, account.getLockedAmount());
            ps.setString(3, account.getStatus().name());
            ps.setString(4, account.getAccountNo());
            ps.executeUpdate();
        }
    }

    public void updateBalanceAndStatus(ClubAccount account) {
        try (Connection conn = DBConnection.getConnection()) {
            updateBalanceAndStatus(conn, account);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account: " + e.getMessage(), e);
        }
    }

    private ClubAccount mapRow(ResultSet rs) throws SQLException {
        String accNo = rs.getString("account_no");
        String clubId = rs.getString("club_id");
        String name = rs.getString("account_name");
        String type = rs.getString("account_type");
        BigDecimal bal = rs.getBigDecimal("balance");
        BigDecimal locked = rs.getBigDecimal("locked_amount");
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));

        ClubAccount acc = "MAIN".equalsIgnoreCase(type)
                ? new MainClubAccount(accNo, clubId, name, bal)
                : new EventSubAccount(accNo, clubId, name, bal);
        acc.setLockedAmount(locked != null ? locked : BigDecimal.ZERO);
        acc.setStatus(status);
        return acc;
    }
}
