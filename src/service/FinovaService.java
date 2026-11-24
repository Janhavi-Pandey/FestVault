package service;

import concurrency.ExpenseClaimManager;
import dao.AccountDAO;
import dao.ClubDAO;
import dao.TransactionDAO;
import database.DBConnection;
import exception.InactiveAccountException;
import exception.InsufficientBalanceException;
import model.AccountStatus;
import model.ClubAccount;
import model.EventSubAccount;
import model.MainClubAccount;
import model.TransactionRecord;
import util.InputValidator;
import util.ReceiptFormatter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class FinovaService {
    private final ClubDAO clubDAO = new ClubDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final InMemoryLedgerStore memoryStore = new InMemoryLedgerStore();
    private final ExpenseClaimManager claimManager = new ExpenseClaimManager();

    public void syncFromDatabase() {
        for (ClubAccount acc : accountDAO.findAll()) {
            memoryStore.registerAccount(acc);
        }
    }

    public void createClubWithMainAccount(String clubId, String clubName, String mainAccNo, String mainAccName) {
        clubDAO.save(new String[]{clubId, clubName});
        MainClubAccount mainAcc = new MainClubAccount(mainAccNo, clubId, mainAccName, BigDecimal.ZERO);
        accountDAO.save(mainAcc);
        memoryStore.registerAccount(mainAcc);
    }

    public void createEventAccount(String accNo, String clubId, String accName) {
        EventSubAccount eventAcc = new EventSubAccount(accNo, clubId, accName, BigDecimal.ZERO);
        accountDAO.save(eventAcc);
        memoryStore.registerAccount(eventAcc);
    }

    public void addSponsorFunds(String accountNo, String sponsorName, String bankRef, BigDecimal amount) {
        if (!InputValidator.isValidBankRef(bankRef)) {
            throw new IllegalArgumentException("Invalid bank reference ID.");
        }
        if (!InputValidator.isPositiveAmount(amount)) {
            throw new IllegalArgumentException("Sponsor amount must be positive.");
        }
        if (!memoryStore.registerBankRef(bankRef.trim(), sponsorName)) {
            throw new IllegalArgumentException("Duplicate bank reference ID: " + bankRef);
        }

        ClubAccount acc = accountDAO.findById(accountNo)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNo));
        if (acc.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("Cannot credit INACTIVE account: " + accountNo);
        }

        acc.setBalance(acc.getBalance().add(amount));
        accountDAO.updateBalanceAndStatus(acc);
        memoryStore.registerAccount(acc);

        TransactionRecord txn = new TransactionRecord(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8),
                null, accountNo, "SPONSOR_INFLOW", amount,
                sponsorName, bankRef.trim(), "Sponsor inflow from " + sponsorName,
                "SUCCESS", LocalDateTime.now()
        );
        transactionDAO.save(txn);
        memoryStore.addTransaction(txn);
    }

    public void transferOwnClubFunds(String fromAccNo, String toAccNo, BigDecimal amount, boolean simulateFailure) {
        ClubAccount source = accountDAO.findById(fromAccNo)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromAccNo));
        ClubAccount target = accountDAO.findById(toAccNo)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + toAccNo));

        memoryStore.validateOwnClubTransfer(source, target);
        source.validateSpend(amount);

        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                source.setBalance(source.getBalance().subtract(amount));
                target.setBalance(target.getBalance().add(amount));

                accountDAO.updateBalanceAndStatus(conn, source);

                if (simulateFailure) {
                    throw new SQLException("Simulated mid-transfer failure before crediting target account.");
                }

                accountDAO.updateBalanceAndStatus(conn, target);

                TransactionRecord txn = new TransactionRecord(
                        txnId, fromAccNo, toAccNo, "SUBTEAM_TRANSFER", amount,
                        null, null, "Sub-team allocation to " + toAccNo,
                        "SUCCESS", LocalDateTime.now()
                );
                transactionDAO.save(conn, txn);

                conn.commit();
                memoryStore.registerAccount(source);
                memoryStore.registerAccount(target);
                memoryStore.addTransaction(txn);
            } catch (Exception ex) {
                conn.rollback();
                throw new RuntimeException("Transfer rolled back: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during transfer: " + e.getMessage(), e);
        }
    }

    public String recordVendorExpense(String accountNo, BigDecimal amount, String purpose) {
        ClubAccount acc = accountDAO.findById(accountNo)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNo));
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            acc.validateSpend(amount);
            acc.setBalance(acc.getBalance().subtract(amount));
            accountDAO.updateBalanceAndStatus(acc);
            memoryStore.registerAccount(acc);

            TransactionRecord txn = new TransactionRecord(
                    txnId, accountNo, null, "VENDOR_EXPENSE", amount,
                    null, null, purpose, "SUCCESS", LocalDateTime.now()
            );
            transactionDAO.save(txn);
            memoryStore.addTransaction(txn);

            return ReceiptFormatter.formatExpenseReceipt(
                    txnId, acc.getAccountNo(), acc.getAccountName(),
                    amount, purpose, acc.getSpendableBalance(), "SUCCESS"
            );
        } catch (InactiveAccountException | InsufficientBalanceException ex) {
            TransactionRecord failedTxn = new TransactionRecord(
                    txnId, accountNo, null, "VENDOR_EXPENSE", amount,
                    null, null, purpose + " [" + ex.getMessage() + "]",
                    "FAILED", LocalDateTime.now()
            );
            transactionDAO.save(failedTxn);
            memoryStore.addTransaction(failedTxn);
            throw ex;
        }
    }

    public void setAccountStatus(String accountNo, AccountStatus status) {
        ClubAccount acc = accountDAO.findById(accountNo)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNo));
        acc.setStatus(status);
        accountDAO.updateBalanceAndStatus(acc);
        memoryStore.registerAccount(acc);
    }

    public ExpenseClaimManager getClaimManager() { return claimManager; }
    public InMemoryLedgerStore getMemoryStore() { return memoryStore; }
    public AccountDAO getAccountDAO() { return accountDAO; }
}
