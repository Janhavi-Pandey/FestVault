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

/** DAO for MAIN and EVENT club wallets. Account numbers are generated here. */
public class AccountDAO implements Repository<ClubAccount, String> {
    private static final String ACCOUNT_NUMBER_LOCK = "finova_account_number_sequence";

    /**
     * The connection-scoped MySQL lock is retained until its connection closes, so a generated
     * number cannot be reused before the caller commits its account-creation transaction.
     */
    public ClubAccount createAccount(Connection conn, String clubId, String accountName, String accountType)
            throws SQLException {
        if (!"MAIN".equals(accountType) && !"EVENT".equals(accountType)) {
            throw new IllegalArgumentException("Account type must be MAIN or EVENT.");
        }
        acquireAccountNumberLock(conn);
        long nextNumber;
        String sequenceSql = "SELECT COALESCE(MAX(CAST(SUBSTRING(account_no, 4) AS UNSIGNED)), 1000) + 1 " +
                "FROM club_accounts WHERE account_no REGEXP '^FV-[0-9]+$'";
        try (PreparedStatement ps = conn.prepareStatement(sequenceSql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Could not generate the next account number.");
            nextNumber = rs.getLong(1);
        }
        if (nextNumber > 999_999_999_999L) {
            throw new SQLException("The generated account number exceeds the club_accounts.account_no limit.");
        }

        String accountNo = "FV-" + nextNumber;
        String sql = "INSERT INTO club_accounts " +
                "(account_no, club_id, account_name, account_type, balance, status) " +
                "VALUES (?, ?, ?, ?, 0.00, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNo);
            ps.setString(2, clubId);
            ps.setString(3, accountName);
            ps.setString(4, accountType);
            ps.executeUpdate();
        }
        if ("MAIN".equals(accountType)) return new MainClubAccount(accountNo, clubId, accountName, BigDecimal.ZERO);
        return new EventSubAccount(accountNo, clubId, accountName, BigDecimal.ZERO);
    }

    @Override
    public void save(ClubAccount account) {
        String sql = "INSERT INTO club_accounts " +
                "(account_no, club_id, account_name, account_type, balance, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAccountNo());
            ps.setString(2, account.getClubId());
            ps.setString(3, account.getAccountName());
            ps.setString(4, account.getAccountType());
            ps.setBigDecimal(5, account.getBalance());
            ps.setString(6, account.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save account: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ClubAccount> findById(String accountNo) {
        String sql = "SELECT * FROM club_accounts WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find account: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<ClubAccount> findByIdForUpdate(Connection conn, String accountNo) throws SQLException {
        String sql = "SELECT * FROM club_accounts WHERE account_no = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ClubAccount> findAll() {
        String sql = "SELECT * FROM club_accounts ORDER BY club_id, account_type, account_no";
        List<ClubAccount> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Could not list accounts: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ClubAccount> findByClubId(String clubId) {
        String sql = "SELECT * FROM club_accounts WHERE club_id = ? ORDER BY account_type, account_no";
        List<ClubAccount> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not list club accounts: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ClubAccount> findEventAccountsByClubId(String clubId) {
        String sql = "SELECT * FROM club_accounts WHERE club_id = ? AND account_type = 'EVENT' " +
                "ORDER BY account_no";
        List<ClubAccount> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not list EVENT accounts: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<ClubAccount> findMainByClubId(String clubId) {
        String sql = "SELECT * FROM club_accounts WHERE club_id = ? AND account_type = 'MAIN' LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find the club MAIN account: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<ClubAccount> findMainByClubId(Connection conn, String clubId, boolean forUpdate)
            throws SQLException {
        String sql = "SELECT * FROM club_accounts WHERE club_id = ? AND account_type = 'MAIN' LIMIT 1" +
                (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public AccountStatus toggleStatus(String accountNo) {
        String update = "UPDATE club_accounts SET status = " +
                "CASE WHEN status = 'ACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, accountNo);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Account not found: " + accountNo);
            try (PreparedStatement read = conn.prepareStatement(
                    "SELECT status FROM club_accounts WHERE account_no = ?")) {
                read.setString(1, accountNo);
                try (ResultSet rs = read.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Account disappeared while toggling status: " + accountNo);
                    return AccountStatus.valueOf(rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not toggle account status: " + e.getMessage(), e);
        }
    }

    public void setStatus(Connection conn, String accountNo, AccountStatus status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE club_accounts SET status = ? WHERE account_no = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, accountNo);
            if (ps.executeUpdate() != 1) throw new SQLException("Account not found: " + accountNo);
        }
    }

    public void updateBalance(Connection conn, String accountNo, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE club_accounts SET balance = ? WHERE account_no = ?")) {
            ps.setBigDecimal(1, balance);
            ps.setString(2, accountNo);
            if (ps.executeUpdate() != 1) throw new SQLException("Account not found: " + accountNo);
        }
    }

    public void updateBalance(String accountNo, BigDecimal balance) {
        try (Connection conn = DBConnection.getConnection()) {
            updateBalance(conn, accountNo, balance);
        } catch (SQLException e) {
            throw new RuntimeException("Could not update account balance: " + e.getMessage(), e);
        }
    }

    public void updateBalanceAndStatus(Connection conn, ClubAccount account) throws SQLException {
        String sql = "UPDATE club_accounts SET balance = ?, status = ? WHERE account_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, account.getBalance());
            ps.setString(2, account.getStatus().name());
            ps.setString(3, account.getAccountNo());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Account not found while updating: " + account.getAccountNo());
            }
        }
    }

    public void updateBalanceAndStatus(ClubAccount account) {
        try (Connection conn = DBConnection.getConnection()) {
            updateBalanceAndStatus(conn, account);
        } catch (SQLException e) {
            throw new RuntimeException("Could not update account: " + e.getMessage(), e);
        }
    }

    private void acquireAccountNumberLock(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT GET_LOCK(?, 10)")) {
            ps.setString(1, ACCOUNT_NUMBER_LOCK);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new SQLException("Timed out while generating an account number.");
                }
            }
        }
    }

    private ClubAccount mapRow(ResultSet rs) throws SQLException {
        String type = rs.getString("account_type");
        ClubAccount account;
        if ("MAIN".equals(type)) {
            account = new MainClubAccount(rs.getString("account_no"), rs.getString("club_id"),
                    rs.getString("account_name"), rs.getBigDecimal("balance"));
        } else if ("EVENT".equals(type)) {
            account = new EventSubAccount(rs.getString("account_no"), rs.getString("club_id"),
                    rs.getString("account_name"), rs.getBigDecimal("balance"));
        } else {
            throw new SQLException("Unknown account type: " + type);
        }
        account.setStatus(AccountStatus.valueOf(rs.getString("status")));
        return account;
    }
}
