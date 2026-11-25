package service;

import concurrency.BatchClaimRunner;
import concurrency.ClaimQueue;
import concurrency.ExpenseClaimManager;
import concurrency.ExpenseClaimTask;
import dao.AccountDAO;
import dao.ClubDAO;
import dao.TransactionDAO;
import database.DBConnection;
import exception.InactiveAccountException;
import exception.InsufficientBalanceException;
import exception.InvalidPinException;
import exception.UnauthorizedTransferException;
import io.CsvExporter;
import model.AccountStatus;
import model.CentralAdmin;
import model.Club;
import model.ClubAccount;
import model.EventSubAccount;
import model.FacultyHead;
import model.MainClubAccount;
import model.StudentClubHead;
import model.TransactionRecord;
import model.User;
import util.InputValidator;
import util.ReceiptFormatter;

import java.math.BigDecimal;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Business rules for Finova. Database work is kept in the DAO layer. */
public class FinovaService {
    private final ClubDAO clubDAO = new ClubDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final ExpenseClaimManager claimManager = new ExpenseClaimManager();
    private final ThreadLocal<User> loggedInUser = new ThreadLocal<>();

    public CentralAdmin loginAdmin(String username, String password) {
        loggedInUser.remove();
        CentralAdmin admin = clubDAO.authenticateAdmin(username, password)
                .orElseThrow(() -> new InvalidPinException("Incorrect Central Admin username or password."));
        loggedInUser.set(admin);
        return admin;
    }

    public CentralAdmin loginCentralAdmin(String username, String password) {
        return loginAdmin(username, password);
    }

    public FacultyHead loginFaculty(String clubId, String username, String password) {
        loggedInUser.remove();
        FacultyHead faculty = clubDAO.authenticateFaculty(clubId, username, password)
                .orElseThrow(() -> new InvalidPinException("Club ID or Faculty credentials are incorrect."));
        loggedInUser.set(faculty);
        return faculty;
    }

    public StudentClubHead loginStudent(String clubId, String username, String password) {
        loggedInUser.remove();
        StudentClubHead student = clubDAO.authenticateStudent(clubId, username, password)
                .orElseThrow(() -> new InvalidPinException("Club ID or Student credentials are incorrect."));
        loggedInUser.set(student);
        return student;
    }

    public void logout() {
        loggedInUser.remove();
    }

    public void registerClubWithFaculty(String clubId, String clubName, String facultyName,
                                        String facultyUser, String facultyPass) {
        requireRole("ADMIN");
        requireText(clubId, "Club ID"); requireText(clubName, "Club name");
        requireText(facultyName, "Faculty name"); requireText(facultyUser, "Faculty username");
        requireText(facultyPass, "Faculty password");
        requireMaxLength(clubId, 15, "Club ID");
        requireMaxLength(clubName, 60, "Club name");
        requireMaxLength(facultyName, 60, "Faculty name");
        if (!InputValidator.isValidClubId(clubId)) throw new IllegalArgumentException("Use a Club ID such as CLB-01.");
        if (!InputValidator.isValidUsername(facultyUser)) throw new IllegalArgumentException("Username must be 3-50 characters (letters, numbers, dot, underscore or hyphen).");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                clubDAO.registerClub(conn, clubId, clubName);
                clubDAO.createOrUpdateFacultyUser(conn, clubId, facultyUser, facultyName, facultyPass);
                accountDAO.createAccount(conn, clubId, clubName + " Main Fund", "MAIN");
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not register club and main fund: " + e.getMessage(), e);
        }
    }

    public void registerClub(String clubId, String clubName, String facultyName,
                             String facultyUser, String facultyPass) {
        registerClubWithFaculty(clubId, clubName, facultyName, facultyUser, facultyPass);
    }

    public Club getClub(String clubId) {
        requireClubReadAccess(clubId);
        return clubDAO.findById(clubId).orElseThrow(() -> new IllegalArgumentException("Club not found."));
    }

    public List<Club> getAllClubs() {
        requireRole("ADMIN");
        return clubDAO.findAll();
    }

    public List<ClubAccount> getAllAccounts() {
        requireRole("ADMIN");
        return accountDAO.findAll();
    }

    public List<ClubAccount> getClubAccounts(String clubId) {
        requireClubReadAccess(clubId);
        return accountDAO.findByClubId(clubId);
    }

    public List<TransactionRecord> getAllSponsorInflows() {
        requireRole("ADMIN");
        return transactionDAO.findSponsorInflowsAll();
    }

    public List<TransactionRecord> getAllTransactions() {
        requireRole("ADMIN");
        return transactionDAO.findAll();
    }

    public int exportGlobalAuditCsv() throws IOException {
        requireRole("ADMIN");
        List<TransactionRecord> records = getAllTransactions();
        new CsvExporter().exportTransactions(records);
        return records.size();
    }

    public ClubAccount createTeamSubAccount(String clubId, String teamName) {
        requireClubRole("FACULTY", clubId);
        requireText(teamName, "Team name");
        requireMaxLength(teamName, 60, "Team name");
        teamName = teamName.trim();
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Club club = clubDAO.findById(clubId).orElseThrow(() -> new IllegalArgumentException("Club not found."));
                ClubAccount account = accountDAO.createAccount(conn, clubId, teamName, "EVENT");
                conn.commit();
                return account;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw new RuntimeException("Could not create team wallet: " + e.getMessage(), e); }
    }

    public ClubAccount createTeamAccount(String clubId, String teamName) {
        return createTeamSubAccount(clubId, teamName);
    }

    public void logSponsorInflow(String clubId, String sponsorName, String utrRefId, BigDecimal amount) {
        requireClubRole("FACULTY", clubId);
        requireText(sponsorName, "Sponsor name");
        String normalizedSponsorName = sponsorName.trim();
        requireMaxLength(normalizedSponsorName, 60, "Sponsor name");
        String bankRefId = utrRefId == null ? null : utrRefId.trim();
        if (!InputValidator.isValidBankRef(bankRefId) || bankRefId.length() > 40) {
            throw new IllegalArgumentException("Enter a valid UTR/reference ID of 4-40 characters.");
        }
        requirePositive(amount);
        String txnId = newTransactionId();
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ClubAccount main = accountDAO.findMainByClubId(conn, clubId, true)
                        .orElseThrow(() -> new IllegalArgumentException("Main Fund account was not found for this club."));
                if (main.getStatus() != AccountStatus.ACTIVE) throw new InactiveAccountException("The club Main Fund is INACTIVE.");
                main.setBalance(main.getBalance().add(amount));
                accountDAO.updateBalanceAndStatus(conn, main);
                transactionDAO.save(conn, new TransactionRecord(txnId, clubId, null, main.getAccountNo(),
                        "SPONSOR_INFLOW", amount, normalizedSponsorName, bankRefId,
                        "Sponsor/grant inflow from " + normalizedSponsorName, "SUCCESS", LocalDateTime.now()));
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                if (e instanceof SQLException sqlEx && isDuplicateKey(sqlEx)) {
                    throw new IllegalArgumentException("This UTR/reference ID has already been recorded.");
                }
                throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) {
            if (isDuplicateKey(e)) throw new IllegalArgumentException("This UTR/reference ID has already been recorded.");
            throw new RuntimeException("Could not record sponsor inflow: " + e.getMessage(), e);
        }
    }

    public void recordSponsorInflow(String clubId, String sponsorName, String utrRefId, BigDecimal amount) {
        logSponsorInflow(clubId, sponsorName, utrRefId, amount);
    }

    public void assignOrResetStudentHead(String clubId, String studentName, String studentUser, String studentPass) {
        requireClubRole("FACULTY", clubId);
        requireText(studentName, "Student Lead name"); requireText(studentUser, "Student username"); requireText(studentPass, "Student password");
        requireMaxLength(studentName, 60, "Student Lead name");
        if (!InputValidator.isValidUsername(studentUser)) throw new IllegalArgumentException("Username must be 3-50 characters (letters, numbers, dot, underscore or hyphen).");
        clubDAO.createOrUpdateStudentUser(clubId, studentUser, studentName, studentPass);
    }

    public void setStudentCredentials(String clubId, String studentName, String studentUser, String studentPass) {
        assignOrResetStudentHead(clubId, studentName, studentUser, studentPass);
    }

    public AccountStatus toggleAccountStatus(String accountNo) {
        requireRole("ADMIN");
        return accountDAO.toggleStatus(accountNo);
    }

    /** Move money from this club's MAIN pool to one of its EVENT wallets. */
    public void transferMainToTeam(String clubId, String toTeamAccountNo, BigDecimal amount, boolean simulateError) {
        requireClubRole("STUDENT", clubId);
        requirePositive(amount);
        String txnId = newTransactionId();
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ClubAccount main = accountDAO.findMainByClubId(conn, clubId, true)
                        .orElseThrow(() -> new IllegalArgumentException("Club Main Fund was not found."));
                ClubAccount team = accountDAO.findByIdForUpdate(conn, toTeamAccountNo)
                        .orElseThrow(() -> new IllegalArgumentException("Selected team wallet was not found."));
                validateTeamOwnership(clubId, main, team);
                main.validateSpend(amount);
                if (team.getStatus() != AccountStatus.ACTIVE) throw new InactiveAccountException("Selected team wallet is INACTIVE.");

                main.setBalance(main.getBalance().subtract(amount));
                accountDAO.updateBalanceAndStatus(conn, main);
                if (simulateError) throw new SQLException("Simulated network interruption. The allocation was rolled back.");
                team.setBalance(team.getBalance().add(amount));
                accountDAO.updateBalanceAndStatus(conn, team);
                transactionDAO.save(conn, new TransactionRecord(txnId, clubId, main.getAccountNo(), team.getAccountNo(),
                        "SUBTEAM_TRANSFER", amount, null, null, "Budget allocation to " + team.getAccountName(),
                        "SUCCESS", LocalDateTime.now()));
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) {
            throw new RuntimeException("Budget allocation failed: " + e.getMessage(), e);
        }
    }

    public void allocateTeamBudget(String clubId, String toTeamAccountNo, BigDecimal amount, boolean simulateFailure) {
        transferMainToTeam(clubId, toTeamAccountNo, amount, simulateFailure);
    }

    public String recordVendorExpense(String clubId, String teamAccountNo, BigDecimal amount, String purpose) {
        requireClubRole("STUDENT", clubId);
        requirePositive(amount); requireText(purpose, "Expense purpose");
        requireMaxLength(purpose, 150, "Expense purpose");
        String txnId = newTransactionId();
        RuntimeException businessFailure = null;
        BigDecimal remaining = BigDecimal.ZERO;
        String accountName = "";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ClubAccount account = accountDAO.findByIdForUpdate(conn, teamAccountNo)
                        .orElseThrow(() -> new IllegalArgumentException("Selected team wallet was not found."));
                if (!clubId.equals(account.getClubId()) || !(account instanceof EventSubAccount)) {
                    throw new UnauthorizedTransferException("You can only pay bills from an EVENT wallet belonging to your own club.");
                }
                accountName = account.getAccountName();
                try {
                    account.validateSpend(amount);
                    account.setBalance(account.getBalance().subtract(amount));
                    accountDAO.updateBalanceAndStatus(conn, account);
                    remaining = account.getBalance();
                    transactionDAO.save(conn, new TransactionRecord(txnId, clubId, teamAccountNo, null,
                            "VENDOR_EXPENSE", amount, null, null, purpose, "SUCCESS", LocalDateTime.now()));
                } catch (InactiveAccountException | InsufficientBalanceException e) {
                    businessFailure = e;
                    remaining = account.getBalance();
                    transactionDAO.save(conn, new TransactionRecord(txnId, clubId, teamAccountNo, null,
                            "VENDOR_EXPENSE", amount, null, null, purpose,
                            "FAILED", LocalDateTime.now()));
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw new RuntimeException("Could not process vendor bill: " + e.getMessage(), e); }
        if (businessFailure != null) throw businessFailure;
        return ReceiptFormatter.formatExpenseReceipt(txnId, getClub(clubId).getClubName(), accountName,
                amount, purpose, remaining, "SUCCESS");
    }

    public String payVendorBill(String clubId, String teamAccountNo, BigDecimal amount, String purpose) {
        return recordVendorExpense(clubId, teamAccountNo, amount, purpose);
    }

    /** Execute multiple claims against one team wallet; row lock + synchronized Java checks protect the balance. */
    public int processBatchClaims(String clubId, String accountNo, BigDecimal amountPerClaim, int threadCount)
            throws Exception {
        requireClubRole("STUDENT", clubId);
        requirePositive(amountPerClaim);
        if (threadCount < 1 || threadCount > 100) throw new IllegalArgumentException("Enter between 1 and 100 threads.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ClubAccount account = accountDAO.findByIdForUpdate(conn, accountNo)
                        .orElseThrow(() -> new IllegalArgumentException("Selected team wallet was not found."));
                if (!clubId.equals(account.getClubId()) || !(account instanceof EventSubAccount)) {
                    throw new UnauthorizedTransferException("Selected wallet does not belong to your club.");
                }
                if (account.getStatus() != AccountStatus.ACTIVE) throw new InactiveAccountException("Selected team wallet is INACTIVE.");

                List<ExpenseClaimTask> tasks = new ArrayList<>();
                for (int i = 1; i <= threadCount; i++) {
                    tasks.add(new ExpenseClaimTask(claimManager, account, "CLAIM-" + i,
                            "Coordinator " + i, amountPerClaim));
                }
                int approved = BatchClaimRunner.runBatch(new ClaimQueue(5), tasks, threadCount);
                accountDAO.updateBalanceAndStatus(conn, account);
                for (ExpenseClaimTask task : tasks) {
                    String status = task.isApproved() ? "SUCCESS" : "FAILED";
                    String reason = task.isApproved() ? task.getCoordinatorTeam() :
                            task.getCoordinatorTeam() + " - rejected because the remaining wallet balance was insufficient";
                    transactionDAO.save(conn, new TransactionRecord(newTransactionId(), clubId, accountNo, null,
                            "VENDOR_EXPENSE", amountPerClaim, null, null, reason, status, LocalDateTime.now()));
                }
                conn.commit();
                return approved;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw new RuntimeException("Batch claims could not be saved: " + e.getMessage(), e); }
    }

    public List<TransactionRecord> getClubTransactions(String clubId) {
        requireClubReadAccess(clubId);
        return transactionDAO.findByClubId(clubId);
    }

    public Map<String, BigDecimal> getTeamWiseSpending(String clubId) {
        Map<String, String> accountNames = new java.util.HashMap<>();
        for (ClubAccount account : getClubAccounts(clubId)) {
            accountNames.put(account.getAccountNo(), account.getAccountName());
        }
        return getClubTransactions(clubId).stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .filter(t -> "VENDOR_EXPENSE".equals(t.getTxnType()))
                .collect(Collectors.groupingBy(
                        t -> accountNames.getOrDefault(t.getFromAccount(), "Unknown team"),
                        Collectors.reducing(BigDecimal.ZERO, TransactionRecord::getAmount, BigDecimal::add)));
    }

    public Map<String, BigDecimal> getMonthlySpending(String clubId) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM");
        return getClubTransactions(clubId).stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .filter(t -> "VENDOR_EXPENSE".equals(t.getTxnType()))
                .collect(Collectors.groupingBy(t -> t.getTxnDate().format(format),
                        java.util.TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, TransactionRecord::getAmount, BigDecimal::add)));
    }

    public List<TransactionRecord> getRecentTransactions(String accountNo, int limit) {
        ClubAccount account = accountDAO.findById(accountNo)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNo));
        requireClubReadAccess(account.getClubId());
        return transactionDAO.findRecentByAccount(accountNo, limit);
    }

    public void importPlannedExpense(String clubId, String teamIdentifier, BigDecimal amount, String description) {
        requireClubRole("STUDENT", clubId);
        List<ClubAccount> clubTeams = getClubAccounts(clubId).stream()
                .filter(a -> a instanceof EventSubAccount)
                .collect(Collectors.toList());
        List<ClubAccount> teams = clubTeams.stream()
                .filter(a -> a.getAccountNo().equalsIgnoreCase(teamIdentifier.trim()))
                .collect(Collectors.toList());
        if (teams.isEmpty()) {
            teams = clubTeams.stream()
                    .filter(a -> a.getAccountName().equalsIgnoreCase(teamIdentifier.trim()))
                    .collect(Collectors.toList());
        }
        if (teams.isEmpty()) throw new IllegalArgumentException("No team wallet with that name or account number was found in your club.");
        if (teams.size() > 1) throw new IllegalArgumentException("More than one team has that name. Use its account number instead.");
        recordVendorExpense(clubId, teams.get(0).getAccountNo(), amount, description);
    }

    private void validateTeamOwnership(String clubId, ClubAccount main, ClubAccount team) {
        if (!(main instanceof MainClubAccount) || !(team instanceof EventSubAccount)) {
            throw new UnauthorizedTransferException("Budget allocation must be from the club MAIN pool to an EVENT team wallet.");
        }
        if (!clubId.equals(main.getClubId()) || !clubId.equals(team.getClubId())) {
            throw new UnauthorizedTransferException("Cross-club budget allocation is not allowed.");
        }
        if (main.getStatus() != AccountStatus.ACTIVE || team.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("Both the Main Pool and team wallet must be ACTIVE.");
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (!InputValidator.isPositiveAmount(amount)) throw new IllegalArgumentException("Amount must be greater than Rs. 0.00.");
    }
    private void requireText(String value, String label) {
        if (!InputValidator.isValidText(value)) throw new IllegalArgumentException(label + " is required.");
    }
    private void requireMaxLength(String value, int maxLength, String label) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " cannot exceed " + maxLength + " characters.");
        }
    }

    private void requireRole(String role) {
        User user = loggedInUser.get();
        if (user == null || !role.equals(user.getRole())) {
            throw new UnauthorizedTransferException("This operation requires an authenticated " + role + " user.");
        }
    }

    private void requireClubRole(String role, String clubId) {
        User user = loggedInUser.get();
        if (user == null || !role.equals(user.getRole()) || !Objects.equals(clubId, user.getClubId())) {
            throw new UnauthorizedTransferException("This operation is not allowed for the current role or club.");
        }
    }

    private void requireClubReadAccess(String clubId) {
        User user = loggedInUser.get();
        if (user == null) {
            throw new UnauthorizedTransferException("An authenticated user is required.");
        }
        if ("ADMIN".equals(user.getRole())) return;
        if (("FACULTY".equals(user.getRole()) || "STUDENT".equals(user.getRole()))
                && Objects.equals(clubId, user.getClubId())) return;
        throw new UnauthorizedTransferException("You can only access data for your assigned club.");
    }
    private String newTransactionId() { return "FIN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(); }
    private boolean isDuplicateKey(SQLException e) { return "23000".equals(e.getSQLState()) || e.getErrorCode() == 1062; }
}
